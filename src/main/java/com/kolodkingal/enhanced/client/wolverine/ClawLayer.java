package com.kolodkingal.enhanced.client.wolverine;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.hulk.HulkPlayerRenderer;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.RenderArmEvent;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.wolverine.Claws;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.joml.Matrix4f;

/**
 * Wolverine's claws (3.7): three adamantium blades from between the knuckles of each empty hand, drawn on the player's
 * arms in third person (this layer) and on his forearms in first person ({@link #onRenderArm}). They slide out over
 * {@code Claws.OUT_TICKS} 3 ticks and back over {@code Claws.IN_TICKS} 2 (the client follows the synced switch and
 * eases between ticks with the partial tick, so they never pop); an item in a hand hides that hand's claws. Silver
 * blades with a bright edge, a bevelled tip; the middle one is a little longer. Nothing of them is an item: they cannot
 * be dropped or taken.
 */
public class ClawLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/wolverine/claw.png");
    /** The blade, in the pixels of the arm (y runs down the arm to the hand's end at 10). */
    static final float BASE = 8.6F;
    static final float FLUSH = 9.7F;
    static final float LENGTH = 7.0F;
    static final float MIDDLE_EXTRA = 0.5F;
    static final float WIDTH = 0.42F;
    static final float DEPTH = 1.0F;
    static final float BEVEL = 1.5F;

    /** Each player's claws: how far out at the last tick and at this one (0 in, 1 out). */
    private static final Int2ObjectMap<float[]> OUT = new Int2ObjectOpenHashMap<>();

    public ClawLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    // ---- how far out ----

    /** Every client tick: each player's claws slide toward the switch. */
    public static void tick(ClientLevel level) {
        float outStep = 1.0F / Claws.OUT_TICKS;
        float inStep = 1.0F / Claws.IN_TICKS;
        Int2ObjectMap<float[]> seen = new Int2ObjectOpenHashMap<>();
        for (Player p : level.players()) {
            float[] f = OUT.get(p.getId());
            boolean out = Claws.out(Powers.get(p));
            if (f == null) {
                if (!out) {
                    continue;
                }
                f = new float[2];
            }
            f[0] = f[1];
            f[1] = out ? Math.min(1.0F, f[1] + outStep) : Math.max(0.0F, f[1] - inStep);
            if (f[0] > 0.0F || f[1] > 0.0F) {
                seen.put(p.getId(), f);
            }
        }
        OUT.clear();
        OUT.putAll(seen);
    }

    /** How far out the player's claws are this frame (0 in, 1 out). */
    public static float out(Player player, float partialTick) {
        float[] f = OUT.get(player.getId());
        if (f == null) {
            return 0.0F;
        }
        float k = Mth.lerp(partialTick, f[0], f[1]);
        return k * k * (3.0F - 2.0F * k);
    }

    public static void clear() {
        OUT.clear();
    }

    // ---- third person ----

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
            float headPitch) {
        float out = out(player, partialTicks);
        if (out <= 0.01F || player.isInvisible()) {
            return;
        }
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        boolean rightMain = player.getMainArm() == HumanoidArm.RIGHT;
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        int overlay = OverlayTexture.NO_OVERLAY;
        if (player.getItemBySlot(rightMain ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND).isEmpty()) {
            onArm(poseStack, vc, packedLight, overlay, getParentModel().rightArm, true, slim, out);
        }
        if (player.getItemBySlot(rightMain ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND).isEmpty()) {
            onArm(poseStack, vc, packedLight, overlay, getParentModel().leftArm, false, slim, out);
        }
    }

    /** The claws of one hand, on that arm as it is posed now. */
    static void onArm(PoseStack poseStack, VertexConsumer vc, int light, int overlay, ModelPart arm, boolean right,
            boolean slim, float out) {
        poseStack.pushPose();
        arm.translateAndRotate(poseStack);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        blades(poseStack, vc, light, overlay, right, slim, out);
        poseStack.popPose();
    }

    /** The three blades in the pixels of an arm. */
    static void blades(PoseStack poseStack, VertexConsumer vc, int light, int overlay, boolean right, boolean slim,
            float out) {
        float centre = slim ? (right ? -0.5F : 0.5F) : (right ? -1.0F : 1.0F);
        float spacing = slim ? 0.9F : 1.2F;
        Matrix4f pose = poseStack.last().pose();
        PoseStack.Pose normal = poseStack.last();
        for (int i = -1; i <= 1; i++) {
            float top = FLUSH + (LENGTH + (i == 0 ? MIDDLE_EXTRA : 0.0F)) * out;
            blade(pose, normal, vc, light, overlay, centre + i * spacing, -0.35F, top);
        }
    }

    /** One blade: a thin prism from inside the fist to {@code top}, its tip bevelled toward the back. */
    private static void blade(Matrix4f pose, PoseStack.Pose normal, VertexConsumer vc, int light, int overlay, float x,
            float z, float top) {
        float x0 = x - WIDTH * 0.5F;
        float x1 = x + WIDTH * 0.5F;
        float z0 = z - DEPTH * 0.5F;
        float z1 = z + DEPTH * 0.5F;
        float yb = BASE;
        float tf = top;
        float tb = Math.max(yb + 0.2F, top - BEVEL);
        float v1 = Math.min(1.0F, (tf - yb) / (LENGTH + 2.0F));
        // The cutting edge (front, toward his knuckles' face): the bright stripe of the texture.
        quad(pose, normal, vc, light, overlay, 0, 0, -1, 0.53F, 0.72F, v1,
                x0, yb, z0, x1, yb, z0, x1, tf, z0, x0, tf, z0);
        quad(pose, normal, vc, light, overlay, 0, 0, 1, 0.0F, 0.22F, v1,
                x1, yb, z1, x0, yb, z1, x0, tb, z1, x1, tb, z1);
        quad(pose, normal, vc, light, overlay, -1, 0, 0, 0.0F, 0.5F, v1,
                x0, yb, z1, x0, yb, z0, x0, tf, z0, x0, tb, z1);
        quad(pose, normal, vc, light, overlay, 1, 0, 0, 0.0F, 0.5F, v1,
                x1, yb, z0, x1, yb, z1, x1, tb, z1, x1, tf, z0);
        quad(pose, normal, vc, light, overlay, 0, 1, 1, 0.53F, 0.72F, 1.0F,
                x0, tf, z0, x1, tf, z0, x1, tb, z1, x0, tb, z1);
    }

    private static void quad(Matrix4f pose, PoseStack.Pose normal, VertexConsumer vc, int light, int overlay,
            float nx, float ny, float nz, float u0, float u1, float v1,
            float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
            float dx, float dy, float dz) {
        vertex(pose, normal, vc, ax, ay, az, u0, 0.0F, light, overlay, nx, ny, nz);
        vertex(pose, normal, vc, bx, by, bz, u1, 0.0F, light, overlay, nx, ny, nz);
        vertex(pose, normal, vc, cx, cy, cz, u1, v1, light, overlay, nx, ny, nz);
        vertex(pose, normal, vc, dx, dy, dz, u0, v1, light, overlay, nx, ny, nz);
    }

    private static void vertex(Matrix4f pose, PoseStack.Pose normal, VertexConsumer vc, float x, float y, float z,
            float u, float v, int light, int overlay, float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(overlay).setLight(light)
                .setNormal(normal, nx, ny, nz);
    }

    // ---- first person ----

    /**
     * His forearm in first person: drawn as the game draws it (the skin, the sleeve), then the claws on it. Not for a
     * player the Hulk's body draws (his own forearm), nor without claws out.
     */
    public static void onRenderArm(RenderArmEvent event) {
        if (event.isCanceled()) {
            return;
        }
        AbstractClientPlayer player = event.getPlayer();
        float out = out(player, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (out <= 0.01F || player.isInvisible() || HulkPlayerRenderer.drawsAsHulk(player)) {
            return;
        }
        EntityRenderer<? super AbstractClientPlayer> any = Minecraft.getInstance().getEntityRenderDispatcher()
                .getRenderer(player);
        if (!(any instanceof PlayerRenderer renderer)) {
            return;
        }
        boolean right = event.getArm() == HumanoidArm.RIGHT;
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        ModelPart arm = right ? model.rightArm : model.leftArm;
        ModelPart sleeve = right ? model.rightSleeve : model.leftSleeve;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        // (As PlayerRenderer.renderHand draws it.)
        model.attackTime = 0.0F;
        model.crouching = false;
        model.swimAmount = 0.0F;
        model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        sleeve.visible = player.isModelPartShown(right ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE);
        arm.xRot = 0.0F;
        ResourceLocation skin = player.getSkin().texture();
        arm.render(poseStack, buffers.getBuffer(RenderType.entitySolid(skin)), light, OverlayTexture.NO_OVERLAY);
        sleeve.xRot = 0.0F;
        sleeve.render(poseStack, buffers.getBuffer(RenderType.entityTranslucent(skin)), light,
                OverlayTexture.NO_OVERLAY);
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        onArm(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY,
                arm, right, slim, out);
        event.setCanceled(true);
    }

    /** The first-person hook, after the Hulk's (his forearm is his own). */
    @Subscriber(side = Side.CLIENT)
    public static final class Hooks {
        @Subscribe(priority = Priority.LOW)
        public static void onRenderArm(RenderArmEvent event) {
            ClawLayer.onRenderArm(event);
        }

        private Hooks() {
        }
    }
}

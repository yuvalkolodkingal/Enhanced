package com.kolodkingal.enhanced.client.ironman.nano;

import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.RenderArmEvent;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * The nanotech suits' gear on the body (3.8, lane L4), drawn as light from the synced bits: the Mark 85's <b>energy
 * blades</b> (long, glowing blue, out of the backs of both hands, 2 blocks; the user's "they come out of the hand"),
 * the Mark 50's <b>nano blade</b> (one red-gold blade on the right forearm), the Mark 85's <b>giant energy shield</b>
 * (the curved wall of blue hexagons of {@code mk85_endgame_shield.jpg}, 4 wide and 3 high, before him from the left
 * arm), the Mark 50's round <b>nano shield</b> on the left forearm, and the <b>nano cannon</b>'s muzzles glowing on
 * both fists while it charges. Each grows in over a few ticks when it appears (interpolated with the partial tick) and
 * flickers a little. In first person the arms' pieces are drawn on the first-person arms ({@link #onRenderArm}). Lane
 * L7's suit models may draw solid versions of these parts; this layer is their light.
 */
public class NanoGearLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** The blades' and the Mark 85 shield's blues, the Mark 50's red and gold. */
    static final int BLUE_CORE = 0xEFFFFF;
    static final int BLUE = 0x5FD0FF;
    static final int BLUE_DEEP = 0x1E7FD8;
    static final int GOLD = 0xFFC24A;
    static final int RED = 0xE0342A;
    /** How long a piece takes to form. */
    static final float FORM_TICKS = 5.0F;

    /** When each player's gear appeared (entity id → client game time), per piece. */
    private static final Map<Integer, Long> BLADES = new HashMap<>();
    private static final Map<Integer, Long> SHIELDS = new HashMap<>();
    private static final Map<Integer, Long> CANNONS = new HashMap<>();

    public NanoGearLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /** Whose nanotech suit this is (a custom hero's gear is the Mark 85's), or null. */
    @Nullable
    static SuitKind suitOf(AbstractClientPlayer player, PowerData data) {
        if (data.power() == Power.CUSTOM) {
            return SuitKind.MARK_85;
        }
        if (data.power() != Power.IRON_MAN) {
            return null;
        }
        SuitKind worn = IronSuits.worn(player);
        return worn != null && worn.nanotech() ? worn : null;
    }

    /** How formed a piece is (0-1), from when it appeared; its time is forgotten when it goes. */
    static float formed(Map<Integer, Long> since, AbstractClientPlayer player, boolean on, float time) {
        if (!on) {
            since.remove(player.getId());
            return 0.0F;
        }
        long start = since.computeIfAbsent(player.getId(), k -> (long) Math.floor(time));
        float f = Mth.clamp((time - start) / FORM_TICKS, 0.0F, 1.0F);
        return f * f * (3.0F - 2.0F * f);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        PowerData data = Powers.get(player);
        SuitKind kind = suitOf(player, data);
        if (kind == null) {
            return;
        }
        float time = player.level().getGameTime() + partialTick;
        float blades = formed(BLADES, player, data.is(Live.IRON_BLADES), time);
        float shield = formed(SHIELDS, player, data.is(Live.IRON_SHIELD), time);
        float cannon = formed(CANNONS, player, data.is(Live.IRON_CHARGING), time);
        if (blades <= 0.0F && shield <= 0.0F && cannon <= 0.0F) {
            return;
        }
        VertexConsumer vc = buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        PlayerModel<AbstractClientPlayer> body = getParentModel();
        if (blades > 0.0F) {
            if (kind == SuitKind.MARK_50) {
                onArm(poseStack, body.rightArm, () -> nanoBlade(poseStack, vc, blades, time));
            } else {
                onArm(poseStack, body.rightArm, () -> energyBlade(poseStack, vc, blades, time, false));
                onArm(poseStack, body.leftArm, () -> energyBlade(poseStack, vc, blades, time, true));
            }
        }
        if (shield > 0.0F) {
            if (kind == SuitKind.MARK_50) {
                // Before the left side of his chest, facing out, wherever the arm is in its swing.
                poseStack.pushPose();
                body.body.translateAndRotate(poseStack);
                poseStack.translate(0.3F, 0.3F, -0.62F);
                poseStack.mulPose(Axis.XP.rotation(Mth.HALF_PI));
                roundShield(poseStack, vc, shield, time, 0.0F);
                poseStack.popPose();
            } else {
                poseStack.pushPose();
                body.body.translateAndRotate(poseStack);
                hexShield(poseStack, vc, shield, time);
                poseStack.popPose();
            }
        }
        if (cannon > 0.0F) {
            onArm(poseStack, body.rightArm, () -> muzzle(poseStack, vc, cannon, time, false));
            onArm(poseStack, body.leftArm, () -> muzzle(poseStack, vc, cannon, time, true));
        }
    }

    private static void onArm(PoseStack poseStack, ModelPart arm, Runnable draw) {
        poseStack.pushPose();
        arm.translateAndRotate(poseStack);
        draw.run();
        poseStack.popPose();
    }

    // ---- the pieces (in the space of the limb, pixels / 16) ----

    /**
     * The Mark 85's energy blade from the back of a hand: a long thin blade of light along the arm and on past the
     * fist, 2 blocks, its core white, its edge blue, a little forward of the knuckles.
     */
    static void energyBlade(PoseStack poseStack, VertexConsumer vc, float formed, float time, boolean left) {
        float flicker = 0.88F + 0.12F * Mth.sin(time * 1.9F + (left ? 1.3F : 0.0F));
        float length = 32.0F * formed;
        poseStack.pushPose();
        // From the back of the hand, swept a little forward and out (the ref's blades flare from the wrists).
        poseStack.translate((left ? 1.0F : -1.0F) / 16.0F, 9.0F / 16.0F, -1.6F / 16.0F);
        poseStack.mulPose(Axis.XP.rotation(-0.45F));
        poseStack.mulPose(Axis.ZP.rotation(left ? -0.28F : 0.28F));
        Matrix4f m = poseStack.last().pose();
        // The glow: a wide, faint blade; the body; the white core.
        blade(m, vc, 0.0F, 0.0F, 0.0F, length, 3.8F, 1.2F, BLUE_DEEP, 0.32F * flicker);
        blade(m, vc, 0.0F, 0.0F, 0.0F, length, 2.3F, 0.7F, BLUE, 0.75F * flicker);
        blade(m, vc, 0.0F, 0.0F, 0.0F, length * 0.97F, 0.9F, 0.3F, BLUE_CORE, 0.95F);
        poseStack.popPose();
    }

    /** The Mark 50's nano blade: a red-gold blade forming from the right forearm past the fist. */
    static void nanoBlade(PoseStack poseStack, VertexConsumer vc, float formed, float time) {
        float shimmer = 0.9F + 0.1F * Mth.sin(time * 2.3F);
        float length = 20.0F * formed;
        Matrix4f m = poseStack.last().pose();
        blade(m, vc, -1.0F, 6.0F, -2.0F, length, 2.2F, 0.6F, RED, 0.6F * shimmer);
        blade(m, vc, -1.0F, 6.0F, -2.0F, length * 0.96F, 1.0F, 0.3F, GOLD, 0.9F);
    }

    /**
     * A blade along the limb's +y from (x, y, z) pixels, {@code length} long, {@code width} wide at the root
     * tapering to a point, {@code thick} thick: two crossed quads and the tip.
     */
    static void blade(Matrix4f m, VertexConsumer vc, float x, float y, float z, float length, float width, float thick,
                      int rgb, float alpha) {
        if (length <= 0.1F) {
            return;
        }
        float s = 1.0F / 16.0F;
        float y0 = y * s;
        float y1 = (y + length) * s;
        float tip = (y + length + width) * s;
        float hw = width * 0.5F * s;
        float ht = thick * 0.5F * s;
        float px = x * s;
        float pz = z * s;
        int a = (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255);
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        // The flat of the blade (facing forward and back) and its edge (facing the sides).
        quad(m, vc, px - ht, y0, pz - hw, px + ht, y0, pz + hw, px + ht, y1, pz + hw, px - ht, y1, pz - hw, r, g, b, a);
        quad(m, vc, px - hw, y0, pz - ht, px + hw, y0, pz + ht, px + hw, y1, pz + ht, px - hw, y1, pz - ht, r, g, b, a);
        quad(m, vc, px - ht, y1, pz - hw, px + ht, y1, pz + hw, px, tip, pz, px, tip, pz, r, g, b, a);
        quad(m, vc, px - hw, y1, pz - ht, px + hw, y1, pz + ht, px, tip, pz, px, tip, pz, r, g, b, a);
    }

    /**
     * The Mark 50's round nano shield: a disc of red-gold light in the limb's x-z plane, {@code at} pixels down it (11:
     * across the first-person arm at the fist, facing along it; 0: at the point the caller turned it to).
     */
    static void roundShield(PoseStack poseStack, VertexConsumer vc, float formed, float time, float at) {
        poseStack.pushPose();
        poseStack.translate(1.0F / 16.0F * Math.signum(at), at / 16.0F, 0.0F);
        Matrix4f m = poseStack.last().pose();
        float radius = 10.0F / 16.0F * formed;
        int segments = 20;
        float pulse = 0.85F + 0.15F * Mth.sin(time * 1.4F);
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments;
            float a1 = Mth.TWO_PI * (i + 1) / segments;
            float x0 = Mth.cos(a0) * radius;
            float z0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius;
            float z1 = Mth.sin(a1) * radius;
            // The face (red), the rim (gold).
            quad(m, vc, 0, 0, 0, x0 * 0.85F, 0, z0 * 0.85F, x1 * 0.85F, 0, z1 * 0.85F, 0, 0, 0, 0xE0, 0x34, 0x2A,
                    (int) (110 * pulse));
            quad(m, vc, x0 * 0.85F, 0, z0 * 0.85F, x0, 0, z0, x1, 0, z1, x1 * 0.85F, 0, z1 * 0.85F, 0xFF, 0xC2,
                    0x4A, (int) (220 * pulse));
        }
        poseStack.popPose();
    }

    /**
     * The Mark 85's giant energy shield, in the body's space: a wall of hexagons 4 blocks wide and 3 high, curved round
     * him, centred a block before his chest and a little to his left; translucent blue cells with bright edges, a
     * ripple of light running across them.
     */
    static void hexShield(PoseStack poseStack, VertexConsumer vc, float formed, float time) {
        poseStack.pushPose();
        poseStack.translate(0.15F, 0.0F, -1.0F);
        poseStack.scale(formed, formed, 1.0F);
        Matrix4f m = poseStack.last().pose();
        float r = 0.32F;
        float w = (float) (Math.sqrt(3.0) * r);
        int cols = 7;
        int rows = 7;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                float cx = (col - (cols - 1) / 2.0F) * w + (row % 2 == 0 ? 0.0F : w * 0.5F) - w * 0.25F;
                float cy = (row - (rows - 1) / 2.0F) * r * 1.5F;
                if (Math.abs(cx) > 2.0F || Math.abs(cy) > 1.5F) {
                    continue;
                }
                float ripple = 0.5F + 0.5F * Mth.sin(time * 0.35F - (cx * 1.3F + cy * 0.7F));
                hexagon(m, vc, cx, cy, r * 0.93F, ripple);
            }
        }
        poseStack.popPose();
    }

    /** One cell of the shield at (cx, cy), curved back at its sides (z grows with x squared). */
    private static void hexagon(Matrix4f m, VertexConsumer vc, float cx, float cy, float r, float ripple) {
        float[] xs = new float[6];
        float[] ys = new float[6];
        for (int i = 0; i < 6; i++) {
            float a = Mth.TWO_PI * i / 6.0F + Mth.PI / 6.0F;
            xs[i] = cx + Mth.cos(a) * r;
            ys[i] = cy + Mth.sin(a) * r;
        }
        float cz = curve(cx);
        int fill = (int) (22 + 38 * ripple);
        int edge = (int) (120 + 110 * ripple);
        for (int i = 0; i < 6; i++) {
            int j = (i + 1) % 6;
            float z0 = curve(xs[i]);
            float z1 = curve(xs[j]);
            quad(m, vc, cx, -cy, cz, xs[i], -ys[i], z0, xs[j], -ys[j], z1, cx, -cy, cz, 0x2F, 0x8F, 0xE8, fill);
            // The bright rim of the cell.
            float ix0 = cx + (xs[i] - cx) * 0.86F;
            float iy0 = cy + (ys[i] - cy) * 0.86F;
            float ix1 = cx + (xs[j] - cx) * 0.86F;
            float iy1 = cy + (ys[j] - cy) * 0.86F;
            quad(m, vc, ix0, -iy0, curve(ix0), xs[i], -ys[i], z0, xs[j], -ys[j], z1, ix1, -iy1, curve(ix1), 0x8F,
                    0xE4, 0xFF, edge);
        }
    }

    /** How far back (towards him: +z) the shield curves at {@code x} blocks from its middle. */
    private static float curve(float x) {
        return 0.12F * x * x;
    }

    /** The nano cannon's muzzle glowing before a fist while it charges. */
    static void muzzle(PoseStack poseStack, VertexConsumer vc, float formed, float time, boolean left) {
        Matrix4f m = poseStack.last().pose();
        float s = 1.0F / 16.0F;
        float x = left ? 1.0F * s : -1.0F * s;
        float y = 12.5F * s;
        float pulse = 0.75F + 0.25F * Mth.sin(time * 2.6F);
        float r = 2.6F * s * formed * pulse;
        int segments = 12;
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments;
            float a1 = Mth.TWO_PI * (i + 1) / segments;
            quad(m, vc, x, y, 0, x + Mth.cos(a0) * r, y, Mth.sin(a0) * r, x + Mth.cos(a1) * r, y, Mth.sin(a1) * r, x,
                    y, 0, 0xCF, 0xF6, 0xFF, (int) (200 * formed));
            quad(m, vc, x + Mth.cos(a0) * r, y, Mth.sin(a0) * r, x + Mth.cos(a0) * r * 1.6F, y,
                    Mth.sin(a0) * r * 1.6F, x + Mth.cos(a1) * r * 1.6F, y, Mth.sin(a1) * r * 1.6F,
                    x + Mth.cos(a1) * r, y, Mth.sin(a1) * r, 0x4F, 0xB8, 0xFF, (int) (90 * formed));
        }
    }

    private static void quad(Matrix4f m, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3, int r, int g, int b, int a) {
        vc.addVertex(m, x0, y0, z0).setColor(r, g, b, a);
        vc.addVertex(m, x1, y1, z1).setColor(r, g, b, a);
        vc.addVertex(m, x2, y2, z2).setColor(r, g, b, a);
        vc.addVertex(m, x3, y3, z3).setColor(r, g, b, a);
    }

    // ---- first person ----

    /**
     * His arms in first person (after they and their suit pieces are drawn): the blades, the round shield and the
     * cannon's muzzles on them. The giant shield is drawn in the world before him (third person only: in first person
     * a faint wall would only hide the view).
     */
    @Subscriber(side = Side.CLIENT)
    public static final class Hooks {
        @Subscribe(priority = Priority.LOW, receiveCanceled = true)
        public static void onRenderArm(RenderArmEvent event) {
            AbstractClientPlayer player = event.getPlayer();
            PowerData data = Powers.get(player);
            SuitKind kind = suitOf(player, data);
            if (kind == null || player.isInvisible()) {
                return;
            }
            EntityRenderer<? super AbstractClientPlayer> any = Minecraft.getInstance().getEntityRenderDispatcher()
                    .getRenderer(player);
            if (!(any instanceof PlayerRenderer renderer)) {
                return;
            }
            float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
            float time = player.level().getGameTime() + partialTick;
            boolean right = event.getArm() == HumanoidArm.RIGHT;
            float blades = formed(BLADES, player, data.is(Live.IRON_BLADES), time);
            float shield = formed(SHIELDS, player, data.is(Live.IRON_SHIELD), time);
            float cannon = formed(CANNONS, player, data.is(Live.IRON_CHARGING), time);
            ModelPart arm = right ? renderer.getModel().rightArm : renderer.getModel().leftArm;
            PoseStack poseStack = event.getPoseStack();
            VertexConsumer vc = event.getMultiBufferSource().getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
            poseStack.pushPose();
            arm.translateAndRotate(poseStack);
            if (blades > 0.0F) {
                if (kind == SuitKind.MARK_85) {
                    energyBlade(poseStack, vc, blades, time, !right);
                } else if (right) {
                    nanoBlade(poseStack, vc, blades, time);
                }
            }
            if (shield > 0.0F && kind == SuitKind.MARK_50 && !right) {
                roundShield(poseStack, vc, shield, time, 11.0F);
            }
            if (cannon > 0.0F) {
                muzzle(poseStack, vc, cannon, time, !right);
            }
            poseStack.popPose();
        }

        private Hooks() {
        }
    }

    /** Forgets every player's gear times (the client left the world). */
    public static void clear() {
        BLADES.clear();
        SHIELDS.clear();
        CANNONS.clear();
    }
}

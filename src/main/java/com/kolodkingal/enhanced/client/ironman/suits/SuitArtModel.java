package com.kolodkingal.enhanced.client.ironman.suits;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.ironman.SuitModel;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.ironman.SuitParts;
import com.kolodkingal.enhanced.ironman.SuitTimelines;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * One suit's model (3.8, PLAN 7.1): the baked {@link SuitMeshes} layer, its texture and its emissive texture. Each part
 * is drawn segment by segment (the nanite wave reveals them one by one, growing each out of the end nearest the reactor
 * from a thin film) with its extras posed by the {@link SuitLook} (the Mark 3's pods, the Mark 50's blade, shield and
 * cannon, the Mark 85's back flaps and energy blades, the Hulkbuster's hinged shells). The plain {@link SuitModel}
 * call draws a part whole, extras folded away.
 */
public final class SuitArtModel implements SuitModel {
    private final SuitKind kind;
    private final SuitMeshes.Info info;
    private final ResourceLocation texture;
    private final ResourceLocation glow;
    private final Map<SuitParts, ModelPart[]> segments = new EnumMap<>(SuitParts.class);
    private final Map<SuitParts, Map<String, ModelPart>> extras = new EnumMap<>(SuitParts.class);

    SuitArtModel(SuitKind kind, ModelPart root, SuitMeshes.Info info) {
        this.kind = kind;
        this.info = info;
        this.texture = Enhanced.id("textures/entity/iron_man/" + kind.id() + ".png");
        this.glow = Enhanced.id("textures/entity/iron_man/" + kind.id() + "_glow.png");
        for (SuitParts part : SuitParts.VALUES) {
            ModelPart p = root.getChild(name(part));
            ModelPart[] segs = new ModelPart[info.segments()[part.ordinal()]];
            for (int i = 0; i < segs.length; i++) {
                segs[i] = p.getChild("s" + i);
            }
            segments.put(part, segs);
            Map<String, ModelPart> x = new HashMap<>();
            for (String extra : SuitArt.EXTRAS) {
                if (p.hasChild("x_" + extra)) {
                    x.put(extra, p.getChild("x_" + extra));
                }
            }
            extras.put(part, x);
        }
    }

    static String name(SuitParts part) {
        return part.name().toLowerCase(java.util.Locale.ROOT);
    }

    public SuitKind kind() {
        return kind;
    }

    @Override
    public ResourceLocation texture() {
        return texture;
    }

    @Override
    public ResourceLocation glowTexture() {
        return glow;
    }

    /**
     * The draw of a caller that knows only the part (the Iron Man lane's player layer and first-person arm, the
     * Hulkbuster lane's renderer swap): as the player being drawn shows it ({@link SuitRenderContext}: the nanite
     * wave's segments, the extras), or the whole part with its extras folded away when no player is being drawn. The
     * Hulkbuster drawn on a player's skeleton is moved onto its own wide shoulders and hips.
     */
    @Override
    public void renderPart(SuitParts part, PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        boolean moved = kind == SuitKind.HULKBUSTER && !SuitRenderContext.firstPerson();
        if (moved) {
            pose.pushPose();
            float[] d = HulkbusterModel.fromPlayer(part);
            pose.translate(d[0] / 16.0F, d[1] / 16.0F, d[2] / 16.0F);
        }
        SuitLook look = SuitRenderContext.look(kind);
        if (look != null) {
            boolean slimArm = look.slim && !moved && SuitRender.arm(part);
            if (slimArm) {
                pose.pushPose();
                SuitRender.slimShift(pose, part);
            }
            renderPart(part, look, pose, buffer, light, overlay, color, false);
            if (slimArm) {
                pose.popPose();
            }
        } else {
            for (ModelPart seg : segments.get(part)) {
                seg.render(pose, buffer, light, overlay, color);
            }
        }
        if (moved) {
            pose.popPose();
        }
    }

    /**
     * Draws one part as the look says: each segment as far as the phase has revealed it (a segment on its way grows
     * out of its pivot along the part's axis from a thin film), the hinged shells as open as {@code look.frameOpen},
     * and the extras the look calls for.
     *
     * @param film true for the nanite film pass: only the segments still forming, as a glow
     */
    void renderPart(SuitParts part, SuitLook look, PoseStack pose, VertexConsumer buffer, int light, int overlay,
                    int color, boolean film) {
        ModelPart[] segs = segments.get(part);
        int axis = info.axes()[part.ordinal()];
        float[] hinges = info.hinges()[part.ordinal()];
        for (int i = 0; i < segs.length; i++) {
            float r = look.inPhase ? SuitTimelines.segment(kind, part, i, segs.length, look.progress, look.up) : 1.0F;
            if (r <= 0.0F || film && (r >= 1.0F || !kind.nanotech())) {
                continue;
            }
            ModelPart seg = segs[i];
            if (kind.nanotech() && r < 1.0F) {
                float grow = SuitTimelines.smooth(r);
                float thin = 0.55F + 0.45F * grow;
                seg.xScale = axis == 0 ? Math.max(0.02F, grow) : thin;
                seg.yScale = axis == 1 ? Math.max(0.02F, grow) : thin;
                seg.zScale = axis == 2 ? Math.max(0.02F, grow) : thin;
            }
            if (hinges != null && hinges[i * 2] >= 0.0F && look.frameOpen > 0.0F) {
                float angle = hinges[i * 2 + 1] * look.frameOpen;
                switch ((int) hinges[i * 2]) {
                    case 0 -> seg.xRot = angle;
                    case 1 -> seg.yRot = angle;
                    default -> seg.zRot = angle;
                }
            }
            seg.render(pose, buffer, light, overlay, color);
            seg.resetPose();
            seg.xScale = seg.yScale = seg.zScale = 1.0F;
        }
        if (film) {
            return;
        }
        Map<String, ModelPart> x = extras.get(part);
        if (!x.isEmpty()) {
            renderExtras(part, x, look, pose, buffer, light, overlay, color);
        }
    }

    /** The extras of a part that the look calls for, posed. */
    private void renderExtras(SuitParts part, Map<String, ModelPart> x, SuitLook look, PoseStack pose,
                              VertexConsumer buffer, int light, int overlay, int color) {
        boolean right = part.right();
        float side = right ? -1.0F : 1.0F;
        for (Map.Entry<String, ModelPart> e : x.entrySet()) {
            ModelPart m = e.getValue();
            String name = e.getKey();
            boolean energy = name.equals("shield") || name.equals("eblade");
            boolean solid = name.equals("blade") || name.equals("cannon");
            if (energy && !SuitArt.ENERGY_GEAR || solid && !SuitArt.NANO_GEAR) {
                continue;
            }
            float amount;
            switch (e.getKey()) {
                case "pod" -> {
                    // the lid turns up over the shoulder's missiles
                    m.xRot = -1.1F * look.pods;
                    amount = 1.0F;
                }
                case "blade" -> {
                    amount = look.blades;
                    m.yScale = Math.max(0.02F, amount);
                }
                case "shield" -> {
                    amount = look.shield;
                    m.xScale = 1.0F;
                    m.yScale = m.zScale = Math.max(0.02F, amount);
                }
                case "cannon" -> {
                    amount = look.cannon;
                    m.xScale = m.zScale = 0.6F + 0.4F * amount;
                    m.yScale = Math.max(0.02F, amount);
                }
                case "eblade" -> {
                    amount = look.blades;
                    m.yScale = Math.max(0.02F, amount);
                    m.zRot = side * 0.12F;
                    m.xRot = 0.10F;
                }
                default -> {
                    // the Mark 85's back flaps: flap0 the outer pair, flap1 the inner; they fan out and back
                    boolean outer = e.getKey().startsWith("flap0");
                    boolean flapRight = e.getKey().endsWith("r");
                    amount = 1.0F;
                    float open = look.flaps;
                    float fan = (outer ? 1.05F : 0.45F) * open;
                    m.zRot = flapRight ? -fan : fan;
                    m.xRot = 0.35F * open;
                }
            }
            if (amount > 0.01F) {
                m.render(pose, buffer, light, overlay, color);
            }
            m.resetPose();
            m.xScale = m.yScale = m.zScale = 1.0F;
        }
    }

    /**
     * A part whole and still, lit by {@code color} on the glow pass (the Hulkbuster's arm that Veronica drops, and
     * other lone pieces).
     */
    void renderStill(SuitParts part, PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        for (ModelPart seg : segments.get(part)) {
            seg.render(pose, buffer, light, overlay, color);
        }
    }
}

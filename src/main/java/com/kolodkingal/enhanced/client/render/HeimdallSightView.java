package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.client.EntityGlow;
import com.kolodkingal.enhanced.config.HeimdallConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.heimdall.HeimdallAbilities;
import com.kolodkingal.enhanced.hela.HelaShadowEntity;
import com.kolodkingal.enhanced.network.HeimdallSightPayload;
import com.kolodkingal.enhanced.platform.client.ClientRegistrar;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ClientPlayerNetworkEvent;
import com.kolodkingal.enhanced.platform.event.client.ComputeFovModifierEvent;
import com.kolodkingal.enhanced.platform.event.client.FMLClientSetupEvent;
import com.kolodkingal.enhanced.platform.event.client.InputEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderLevelStageEvent;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModTags;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What Heimdall's All-Seeing Sight shows on his client (3.5), and what the Guardian's Watch lends its watcher:
 * <ul>
 * <li>every living thing within {@code heimdall.sightRange} glows through walls ({@link EntityGlow}): players gold, monsters
 * red, animals green, Asgardians blue, the Shadows of Hela gold;</li>
 * <li>every far player of his realm is marked where he stands, with his name and distance, and those of the other realms
 * are listed at the side of the screen ("Peter — The Nether"), from {@link HeimdallSightPayload};</li>
 * <li>during the Watch, the shadows still hidden are marked in gold through every wall, and a needle at the top of the
 * screen points to the nearest;</li>
 * <li>Far Sight (Sneak + G held) zooms the view ×6, and the mouse wheel takes it up to ×10 and back.</li>
 * </ul>
 */
@Subscriber(side = Side.CLIENT)
public final class HeimdallSightView {
    public static final int PLAYER = 0xFFD24A;
    public static final int MONSTER = 0xFF4A3A;
    public static final int ANIMAL = 0x5CE06A;
    public static final int ASGARDIAN = 0x5AA8FF;
    public static final int OTHER = 0xFFE7A3;
    /** Far Sight's zoom: from and to, and the step of a turn of the wheel. */
    public static final float ZOOM_MIN = 6.0F;
    public static final float ZOOM_MAX = 10.0F;
    private static final float ZOOM_STEP = 0.5F;

    private static List<HeimdallSightPayload.Far> far = List.of();
    private static List<Vec3> shadows = List.of();
    private static int watchSeconds = -1;
    private static float zoomTarget = ZOOM_MIN;
    private static float zoom = 1.0F;

    /** The server's latest list of far players. */
    public static void receive(HeimdallSightPayload payload) {
        far = List.copyOf(payload.players());
    }

    /** The Watch's shadows still hidden and its seconds left ({@code seconds} -1: it is over). */
    static void watch(List<Vec3> points, int seconds) {
        watchSeconds = seconds;
        shadows = seconds < 0 ? List.of() : List.copyOf(points);
    }

    /** How many far players of his realm the Sight marks now. */
    static int farCount() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return 0;
        }
        int n = 0;
        for (HeimdallSightPayload.Far f : far) {
            n += f.dimension().equals(mc.level.dimension().location()) ? 1 : 0;
        }
        return n;
    }

    /** Whether the local player keeps a Watch (as his client knows it). */
    public static boolean watching() {
        return watchSeconds >= 0;
    }

    /** Whether the Sight is open for the local player: his own, or the one the Watch lends him. */
    public static boolean active() {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null) {
            return false;
        }
        PowerData data = Powers.get(me);
        return data.has(HeroAbility.HEIMDALL_SIGHT) && (data.toggle(Toggle.HEIMDALL_SIGHT) || data.is(Live.HEIMDALL_FARSIGHT)) || watching();
    }

    private static boolean farSight() {
        LocalPlayer me = Minecraft.getInstance().player;
        return me != null && Powers.get(me).is(Live.HEIMDALL_FARSIGHT) && Powers.get(me).has(HeroAbility.HEIMDALL_SIGHT);
    }

    private static double range() {
        return EnhancedConfig.SPEC.isLoaded() ? HeimdallConfig.SIGHT_RANGE.get() : 96;
    }

    // ---- the glow ----

    /** Whether the Sight shows this entity through walls. */
    static boolean reveals(Entity e) {
        if (!(e instanceof LivingEntity) || !active()) {
            return false;
        }
        LocalPlayer me = Minecraft.getInstance().player;
        if (e == me || e.isInvisible() && !(e instanceof HelaShadowEntity)) {
            return false;
        }
        double r = range();
        return e instanceof HelaShadowEntity || e.distanceToSqr(me) <= r * r;
    }

    /** The colour it glows in. */
    static int color(Entity e) {
        if (e instanceof Player || e instanceof HelaShadowEntity) {
            return PLAYER;
        }
        if (e.getType().is(ModTags.ASGARDIANS)) {
            return ASGARDIAN;
        }
        if (e instanceof Enemy) {
            return MONSTER;
        }
        return e instanceof Animal ? ANIMAL : OTHER;
    }

    // ---- setup and the client's life ----

    /** The glow source and the drawers of Heimdall's effects (the mod bus: the client's setup). */
    @Subscriber(side = Side.CLIENT)
    public static final class Setup {
        @Subscribe
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                EntityGlow.register(HeimdallSightView::reveals, HeimdallSightView::color);
                HeimdallEffects.register();
            });
        }

        /** The sight's HUD layer (port P0: through the platform's client registrar, see ClientSetup). */
        public static void register(ClientRegistrar registrar) {
            registrar.guiLayer(Enhanced.id("heimdall_sight"), ClientRegistrar.HudAnchor.ABOVE,
                    ClientRegistrar.VanillaHud.CAMERA_OVERLAYS, HeimdallSightView::drawHud);
        }

        private Setup() {
        }
    }

    static void tick(Minecraft mc) {
        float target = farSight() ? zoomTarget : 1.0F;
        zoom += (target - zoom) * 0.35F;
        if (Math.abs(target - zoom) < 0.01F) {
            zoom = target;
        }
        if (!active() && !far.isEmpty()) {
            far = List.of();
        }
    }

    static void clear() {
        far = List.of();
        shadows = List.of();
        watchSeconds = -1;
        zoom = 1.0F;
        zoomTarget = ZOOM_MIN;
    }

    @Subscribe
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    // ---- Far Sight ----

    @Subscribe
    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (zoom > 1.001F && event.getPlayer() == Minecraft.getInstance().player) {
            event.setNewFovModifier(event.getNewFovModifier() / zoom);
        }
    }

    @Subscribe
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (farSight() && Minecraft.getInstance().screen == null) {
            zoomTarget = Mth.clamp(zoomTarget + (float) Math.signum(event.getScrollDeltaY()) * ZOOM_STEP, ZOOM_MIN, ZOOM_MAX);
            event.setCanceled(true);
        }
    }

    // ---- the marks in the world ----

    @Subscribe
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !active()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer me = mc.player;
        if (mc.level == null || me == null || mc.options.hideGui) {
            return;
        }
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = mc.level.getGameTime() + pt;
        Camera view = event.getCamera();
        Vec3 camera = view.getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Matrix4f identity = new Matrix4f();
        ResourceLocation here = mc.level.dimension().location();
        List<Vec3> beams = new ArrayList<>();
        for (HeimdallSightPayload.Far f : far) {
            if (f.dimension().equals(here) && mc.level.getPlayerByUUID(f.id()) == null) {
                beams.add(f.pos());
            }
        }
        VertexConsumer vc = buffers.getBuffer(EnhancedRenderTypes.BUILD_XRAY);
        for (Vec3 at : beams) {
            Vec3 m = marker(camera, at).subtract(camera);
            double scale = Math.max(1.0, m.length() / 16.0);
            EnergyGeometry.ribbon(identity, vc, m, m.add(0, 2.2 * scale, 0), (float) (0.25 * scale), (float) (0.05 * scale),
                    EnergyGeometry.argb(PLAYER, 0.65F), EnergyGeometry.argb(PLAYER, 0.0F));
        }
        for (Vec3 at : shadows) {
            Vec3 m = marker(camera, at.add(0, 0.4, 0)).subtract(camera);
            double scale = Math.max(1.0, m.length() / 12.0);
            float pulse = 0.6F + 0.4F * Mth.sin(time * 0.3F);
            diamond(identity, vc, m, (float) (0.35 * scale), EnergyGeometry.argb(HeimdallAbilities.GOLD, 0.7F * pulse));
            EnergyGeometry.ribbon(identity, vc, m, m.add(0, 6.0 * scale, 0), (float) (0.12 * scale), 0.0F,
                    EnergyGeometry.argb(HeimdallAbilities.LIGHT, 0.5F), EnergyGeometry.argb(HeimdallAbilities.LIGHT, 0.0F));
        }
        buffers.endBatch(EnhancedRenderTypes.BUILD_XRAY);
        Font font = mc.font;
        for (HeimdallSightPayload.Far f : far) {
            if (!f.dimension().equals(here) || mc.level.getPlayerByUUID(f.id()) != null) {
                continue;
            }
            Vec3 where = f.pos().add(0, 2.4, 0);
            Vec3 at = marker(camera, where).subtract(camera);
            float scale = 0.025F * (float) Math.max(1.0, at.length() / 8.0);
            PoseStack stack = new PoseStack();
            stack.translate(at.x, at.y, at.z);
            stack.mulPose(view.rotation());
            stack.scale(scale, -scale, scale);
            Component name = Component.translatable("hud.enhanced.heimdall.far", f.name(), Math.round(where.distanceTo(camera)));
            font.drawInBatch(name, -font.width(name) / 2.0F, 0.0F, 0xE0000000 | PLAYER, false, stack.last().pose(), buffers,
                    Font.DisplayMode.SEE_THROUGH, 0x50000000, LightTexture.FULL_BRIGHT);
        }
        buffers.endBatch();
    }

    /** Where a mark is drawn: never further than 48 blocks, so the world's far plane never hides it. */
    static Vec3 marker(Vec3 eye, Vec3 at) {
        Vec3 d = at.subtract(eye);
        double len = d.length();
        return len <= 48.0 ? at : eye.add(d.scale(48.0 / len));
    }

    private static void diamond(Matrix4f pose, VertexConsumer vc, Vec3 c, float r, int colour) {
        Vec3 top = c.add(0, r * 1.4, 0);
        Vec3 bottom = c.add(0, -r * 1.4, 0);
        Vec3[] ring = {c.add(r, 0, 0), c.add(0, 0, r), c.add(-r, 0, 0), c.add(0, 0, -r)};
        for (int i = 0; i < 4; i++) {
            Vec3 a = ring[i];
            Vec3 b = ring[(i + 1) % 4];
            for (Vec3 tip : new Vec3[]{top, bottom}) {
                vc.addVertex(pose, (float) a.x, (float) a.y, (float) a.z).setColor(colour);
                vc.addVertex(pose, (float) b.x, (float) b.y, (float) b.z).setColor(colour);
                vc.addVertex(pose, (float) tip.x, (float) tip.y, (float) tip.z).setColor(colour);
                vc.addVertex(pose, (float) tip.x, (float) tip.y, (float) tip.z).setColor(colour);
            }
        }
    }

    // ---- the screen: the other realms, the Watch's needle ----

    static void drawHud(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer me = mc.player;
        if (me == null || mc.options.hideGui || !active()) {
            return;
        }
        Font font = mc.font;
        int w = g.guiWidth();
        ResourceLocation here = me.level().dimension().location();
        int y = 6;
        boolean header = false;
        for (HeimdallSightPayload.Far f : far) {
            if (f.dimension().equals(here)) {
                continue;
            }
            if (!header) {
                Component title = Component.translatable("hud.enhanced.heimdall.realms").withStyle(ChatFormatting.GOLD);
                g.drawString(font, title, w - font.width(title) - 6, y, 0xFFFFFF, true);
                y += 11;
                header = true;
            }
            Component line = Component.translatable("hud.enhanced.heimdall.realm", f.name(), realmName(f.dimension()));
            g.drawString(font, line, w - font.width(line) - 6, y, PLAYER, true);
            y += 10;
            if (y > g.guiHeight() / 2) {
                break;
            }
        }
        if (watching() && !shadows.isEmpty()) {
            needle(g, font, me, w);
        }
    }

    /** The Watch's needle: an arrow at the top of the screen turned to the nearest shadow, and how far it is. */
    private static void needle(GuiGraphics g, Font font, LocalPlayer me, int w) {
        Vec3 eye = me.getEyePosition();
        Vec3 nearest = null;
        for (Vec3 s : shadows) {
            if (nearest == null || s.distanceToSqr(eye) < nearest.distanceToSqr(eye)) {
                nearest = s;
            }
        }
        if (nearest == null) {
            return;
        }
        double dx = nearest.x - me.getX();
        double dz = nearest.z - me.getZ();
        float bearing = (float) Math.toDegrees(Math.atan2(-dx, dz)) - me.getYRot();
        bearing = Mth.wrapDegrees(bearing);
        String arrow = arrow(bearing);
        int cx = w / 2;
        int top = 32;
        Component dist = Component.translatable("hud.enhanced.heimdall.shadow", Math.round(Math.sqrt(dx * dx + dz * dz)),
                String.format(Locale.ROOT, "%+d", Math.round(nearest.y - me.getY())));
        g.drawCenteredString(font, Component.literal(arrow).withStyle(ChatFormatting.BOLD), cx, top, HeimdallAbilities.GOLD);
        g.drawCenteredString(font, dist, cx, top + 11, HeimdallAbilities.LIGHT);
    }

    /** One of eight arrows for a bearing (degrees, 0 = straight ahead, positive to the right). */
    static String arrow(float bearing) {
        String[] arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
        int i = Math.floorMod(Math.round(bearing / 45.0F), 8);
        return arrows[i];
    }

    /** "Overworld", "The Nether", "The End", "Asgard", or the id of a dimension added by another mod. */
    private static Component realmName(ResourceLocation id) {
        String key = "dimension." + id.getNamespace() + "." + id.getPath();
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(id.toString());
    }

    private HeimdallSightView() {
    }
}

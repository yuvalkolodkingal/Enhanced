package com.kolodkingal.enhanced.client.custom;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.SuitSkins;
import com.kolodkingal.enhanced.client.render.HeroAnimator;
import com.kolodkingal.enhanced.custom.CatalogEntry;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The live 3D preview of the Hero Creator (3.4): a player who is not in the world, drawn by the game's own player
 * renderer, so the hero being made shows with his skin, his cape and every layer of his look, and moves with the mod's
 * animations. It has a random UUID and the local player's name; its power data is set here, on this client only, to the
 * draft (a custom hero with the draft's kit), and nothing of it reaches the server. The editor ticks it, tells the
 * animator how it moves ({@link HeroAnimator#preview}: standing, walking, running, flying) and what to try
 * ({@link HeroAnimator#previewCast}, {@link HeroAnimator#previewHold}), and draws it at the frame's partial tick (the
 * inventory's drawing uses a partial tick of 1, which the animator leaves alone). Where the animator does not move it, it
 * walks and runs with the game's own limb swing. If drawing it ever fails, {@link #broken} says so (logged once) and the
 * editor draws the paper doll instead.
 */
public final class PreviewPuppet extends RemotePlayer {
    /** How high above the camera it stands: out of reach of name tags and of the world's light and particles. */
    private static final double ABOVE = 300.0;
    private static boolean broken;

    private CustomHero hero = CustomHero.EMPTY;
    private HeroAnimator.PreviewMotion motion = HeroAnimator.PreviewMotion.IDLE;

    private PreviewPuppet(ClientLevel level, GameProfile profile) {
        super(level, profile);
        // Every part of the skin shown, the cape too (a real player's choice reaches others through the server).
        entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7F);
        JsonObject power = new JsonObject();
        power.addProperty("power", "custom");
        setData(ModAttachments.POWER, PowerData.CODEC.parse(JsonOps.INSTANCE, power).getOrThrow());
        noPhysics = true;
    }

    /** A new puppet in the client's world, or null when there is none (or the puppet broke before). */
    @Nullable
    public static PreviewPuppet create() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || broken) {
            return null;
        }
        try {
            PreviewPuppet puppet = new PreviewPuppet(mc.level, new GameProfile(UUID.randomUUID(), mc.player.getGameProfile().getName()));
            puppet.place();
            return puppet;
        } catch (RuntimeException e) {
            fail(e);
            return null;
        }
    }

    /** Whether the puppet failed once on this client (then the editor draws the paper doll). */
    public static boolean broken() {
        return broken;
    }

    private static void fail(Throwable e) {
        if (!broken) {
            broken = true;
            Enhanced.LOGGER.error("The Hero Creator's 3D preview failed; the flat picture is drawn instead", e);
        }
    }

    /** Wears the draft (passed through the rules, so its kit is what the server would make of it). */
    public void wear(CustomHero draft, CustomHeroRules.Limits limits) {
        CustomHero ruled = CustomHeroRules.sanitize(draft, limits).hero();
        if (!ruled.equals(hero)) {
            hero = ruled;
            setData(ModAttachments.CUSTOM_HERO, ruled);
        }
    }

    public CustomHero hero() {
        return hero;
    }

    public void motion(HeroAnimator.PreviewMotion motion) {
        this.motion = motion;
    }

    public HeroAnimator.PreviewMotion motion() {
        return motion;
    }

    /** Plays a binding's gesture, as a cast of it would (a key: 1-5, plain or with Sneak). */
    public void tryBinding(int column, boolean sneak) {
        try {
            HeroAnimator.previewCast(this, column, sneak);
        } catch (RuntimeException e) {
            fail(e);
        }
    }

    /** Raises or lowers the stance of a held or switched-on power. */
    public void tryHold(CatalogEntry entry, boolean on) {
        try {
            HeroAnimator.previewHold(this, entry, on);
        } catch (RuntimeException e) {
            fail(e);
        }
    }

    @Override
    public PlayerSkin getSkin() {
        Minecraft mc = Minecraft.getInstance();
        PlayerSkin own = null;
        if (mc.getConnection() != null && mc.player != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            own = info == null ? null : info.getSkin();
        }
        if (own == null) {
            own = mc.getSkinManager().getInsecureSkin(mc.getGameProfile());
        }
        return SuitSkins.skinFor(this, own);
    }

    /** Stands it above the camera, its cloak (the cape's swing) where it is. */
    private void place() {
        Minecraft mc = Minecraft.getInstance();
        double x = mc.player.getX();
        double y = mc.player.getY() + ABOVE;
        double z = mc.player.getZ();
        setPos(x, y, z);
        xo = xOld = x;
        yo = yOld = y;
        zo = zOld = z;
        xCloak = xCloakO = x;
        yCloak = yCloakO = y;
        zCloak = zCloakO = z;
    }

    /** One tick of the editor: its clock, its walk, the swing of its cape. */
    public void tickPreview() {
        tickCount++;
        place();
        float speed = switch (motion) {
            case WALK -> 0.45F;
            case RUN -> 0.85F;
            case SPEED_RUN -> 1.0F;
            default -> 0.0F;
        };
        walkAnimation.update(speed, 0.4F);
        // The cape lags behind as it would on the move: the cloak a little behind the body.
        float lag = switch (motion) {
            case WALK -> 0.08F;
            case RUN, FLY_CRUISE -> 0.2F;
            case SPEED_RUN, FLY_SPRINT -> 0.35F;
            default -> 0.0F;
        };
        float yaw = yBodyRot * Mth.DEG_TO_RAD;
        xCloakO = xCloak;
        zCloakO = zCloak;
        xCloak = getX() + Mth.sin(yaw) * lag;
        zCloak = getZ() - Mth.cos(yaw) * lag;
        oBob = bob;
        bob = speed * 0.1F;
        walkDistO = walkDist;
        walkDist += speed * 0.6F;
    }

    /**
     * Draws it in a box of the screen: its feet at ({@code x}, {@code y}), {@code size} pixels a block, turned {@code yaw}
     * degrees from facing the viewer. Returns false (and draws nothing) once it has failed.
     */
    public boolean draw(GuiGraphics g, int x, int y, float size, float yaw, float partialTick) {
        if (broken) {
            return false;
        }
        boolean pushed = false;
        try {
            yBodyRot = yBodyRotO = 180.0F + yaw;
            yHeadRot = yHeadRotO = 180.0F + yaw;
            setYRot(180.0F + yaw);
            yRotO = getYRot();
            setXRot(0.0F);
            xRotO = 0.0F;
            HeroAnimator.preview(this, motion, partialTick);
            g.pose().pushPose();
            pushed = true;
            g.pose().translate(x, y, 150.0F);
            g.pose().scale(size, size, -size);
            g.pose().mulPose(Axis.ZP.rotationDegrees(180.0F));
            Lighting.setupForEntityInInventory();
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            dispatcher.setRenderShadow(false);
            RenderSystem.runAsFancy(() -> dispatcher.render(this, 0.0, 0.0, 0.0, 0.0F, partialTick, g.pose(), g.bufferSource(), 0xF000F0));
            g.flush();
            dispatcher.setRenderShadow(true);
            g.pose().popPose();
            pushed = false;
            Lighting.setupFor3DItems();
            return true;
        } catch (RuntimeException e) {
            fail(e);
            if (pushed) {
                g.pose().popPose();
            }
            Lighting.setupFor3DItems();
            return false;
        }
    }
}

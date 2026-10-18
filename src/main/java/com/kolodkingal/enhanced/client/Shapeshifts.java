package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.LokiAbilities;
import com.kolodkingal.enhanced.power.LokiForm;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * How everyone sees Loki: in a creature's shape, the creature is drawn in place of his body (his position, his turn of
 * the head, his walk), with no name tag; in another player's shape he wears that player's skin and shows their name.
 * In first person nothing changes. While he hides behind his duplicates, nothing of him is drawn at all.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class Shapeshifts {
    /** The creature body drawn for each shapeshifted Loki (by player entity id). Never added to the level. */
    private static final Map<Integer, LivingEntity> BODIES = new HashMap<>();
    /** Creature types whose renderer failed with a stand-in body: Loki is drawn as himself instead. */
    private static final Set<EntityType<?>> BROKEN = new HashSet<>();
    /** Guards against two Lokis wearing each other's faces. */
    private static int copying;

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (LokiAbilities.isHidden(player)) {
            event.setCanceled(true);
            return;
        }
        LivingEntity body = body(player);
        if (body == null) {
            return;
        }
        float partialTick = event.getPartialTick();
        follow(player, body);
        try {
            EntityRenderer<? super LivingEntity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(body);
            renderer.render(body, Mth.lerp(partialTick, player.yRotO, player.getYRot()), partialTick, event.getPoseStack(),
                    event.getMultiBufferSource(), event.getPackedLight());
        } catch (RuntimeException e) {
            Enhanced.LOGGER.warn("Couldn't draw Loki as {}; drawing him as himself", EntityType.getKey(body.getType()), e);
            BROKEN.add(body.getType());
            BODIES.remove(player.getId());
            return;
        }
        event.setCanceled(true);
    }

    /** A copied player's name over Loki's head. */
    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (event.getEntity() instanceof Player player) {
            LokiForm form = playerForm(player);
            if (form != null) {
                event.setContent(Component.literal(form.name()));
            }
        }
    }

    /** Keeps each creature body walking in step with its Loki; called every client tick. */
    public static void tick(ClientLevel level) {
        BODIES.entrySet().removeIf(e -> !(level.getEntity(e.getKey()) instanceof Player player)
                || e.getValue().level() != level || body(player) != e.getValue());
        for (Map.Entry<Integer, LivingEntity> e : BODIES.entrySet()) {
            Player player = (Player) level.getEntity(e.getKey());
            LivingEntity body = e.getValue();
            body.tickCount = player.tickCount;
            body.walkAnimation.update(player.walkAnimation.speed(), 1.0F);
        }
    }

    public static void clear() {
        BODIES.clear();
    }

    /** The name Loki shows: a copied player's, or his own. */
    public static Component shownName(Player player) {
        LokiForm form = playerForm(player);
        return form != null ? Component.literal(form.name()) : player.getDisplayName();
    }

    /**
     * The skin of the player Loki copied, or null if he isn't in a player's shape (or it is his own first-person
     * view, which never changes): their current skin if they are nearby, otherwise the skin they joined with.
     */
    @Nullable
    public static PlayerSkin copiedSkin(AbstractClientPlayer loki) {
        LokiForm form = playerForm(loki);
        Minecraft mc = Minecraft.getInstance();
        if (form == null || (loki == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson())) {
            return null;
        }
        if (copying == 0 && mc.level != null && mc.level.getPlayerByUUID(form.player()) instanceof AbstractClientPlayer other && other != loki) {
            copying++;
            try {
                return other.getSkin();
            } finally {
                copying--;
            }
        }
        PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(form.player());
        return info != null ? info.getSkin() : DefaultPlayerSkin.get(form.player());
    }

    @Nullable
    private static LokiForm playerForm(Player player) {
        PowerData data = Powers.get(player);
        if (data.power() != Power.LOKI || data.form().isEmpty()) {
            return null;
        }
        return LokiForm.of(data).filter(LokiForm::isPlayer).orElse(null);
    }

    /** The creature body to draw for this player, made the first time it's needed; null to draw the player. */
    @Nullable
    private static LivingEntity body(Player player) {
        PowerData data = Powers.get(player);
        // (3.3: a custom hero's Shapeshifting "creature" takes a shape the same way.)
        LokiForm form = (data.power() == Power.LOKI || data.power() == Power.CUSTOM) && !data.form().isEmpty() ? LokiForm.of(data).orElse(null) : null;
        if (form == null || form.isPlayer() || BROKEN.contains(form.type())) {
            return null;
        }
        LivingEntity body = BODIES.get(player.getId());
        if (body != null && body.getType() == form.type() && body.level() == player.level()) {
            return body;
        }
        Entity created = form.type().create(player.level());
        if (!(created instanceof LivingEntity living)) {
            BROKEN.add(form.type());
            return null;
        }
        BODIES.put(player.getId(), living);
        return living;
    }

    /** Puts the creature where Loki is, turned and moving as he is, holding what he holds. */
    private static void follow(Player player, LivingEntity body) {
        body.setPos(player.getX(), player.getY(), player.getZ());
        body.xo = player.xo;
        body.yo = player.yo;
        body.zo = player.zo;
        body.xOld = player.xOld;
        body.yOld = player.yOld;
        body.zOld = player.zOld;
        body.setYRot(player.getYRot());
        body.yRotO = player.yRotO;
        body.setXRot(player.getXRot());
        body.xRotO = player.xRotO;
        body.yBodyRot = player.yBodyRot;
        body.yBodyRotO = player.yBodyRotO;
        body.yHeadRot = player.yHeadRot;
        body.yHeadRotO = player.yHeadRotO;
        body.setOnGround(player.onGround());
        body.setInvisible(player.isInvisible());
        body.setSharedFlagOnFire(player.isOnFire());
        body.attackAnim = player.attackAnim;
        body.oAttackAnim = player.oAttackAnim;
        body.hurtTime = player.hurtTime;
        body.hurtDuration = player.hurtDuration;
        body.deathTime = player.deathTime;
        body.setItemSlot(EquipmentSlot.MAINHAND, player.getMainHandItem());
        body.setItemSlot(EquipmentSlot.OFFHAND, player.getOffhandItem());
    }

    private Shapeshifts() {
    }
}

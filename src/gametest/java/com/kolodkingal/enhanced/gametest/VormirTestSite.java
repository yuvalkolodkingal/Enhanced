package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.vormir.VormirSite;
import com.kolodkingal.enhanced.vormir.VormirSites;
import com.kolodkingal.enhanced.vormir.VormirTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A small Vormir Cliff in a GameTest's arena (3.6, lane L5; the plan's 10: not L4's 74-high template, the same parts):
 * in the half of the arena from {@code x0} to {@code x0 + 9}, a stone ledge 4 blocks up whose edge strip ({@code
 * EDGE_BOX}) is 7 x 2 x 2 at relative (x0 + 2..8, 6..7, 10..11), the chasm's shaft ({@code PIT_BOX}) on the floor in
 * front of it at (x0 + 2..8, 2..4, 2..8), the pool on the floor at (x0 + 5, 2, 15) and the keeper's place at (x0 + 5,
 * 2, 18). The site is known to {@link VormirSites} until the test ends. A fall in the arena is a few blocks: the tests
 * give a falling creature the fall distance of the real chasm ({@link #fallDie}).
 */
public final class VormirTestSite {
    private final GameTestHelper helper;
    private final int x0;
    private final VormirSite site;

    private VormirTestSite(GameTestHelper helper, int x0, VormirSite site) {
        this.helper = helper;
        this.x0 = x0;
        this.site = site;
    }

    /** The cliff in the half of the arena from {@code x0} (0 or 11), known until the test ends. */
    public static VormirTestSite build(TestKit.Fixture f, int x0) {
        GameTestHelper helper = f.helper();
        for (int x = x0 + 2; x <= x0 + 8; x++) {
            for (int z = 9; z <= 12; z++) {
                helper.setBlock(new BlockPos(x, 5, z), Blocks.STONE);
            }
        }
        VormirSite site = new VormirSite(helper.getLevel().dimension(),
                box(helper, x0, 1, 0, x0 + 9, 10, 20), box(helper, x0 + 2, 6, 10, x0 + 8, 7, 11),
                box(helper, x0 + 2, 2, 2, x0 + 8, 4, 8), helper.absolutePos(new BlockPos(x0 + 5, 2, 15)),
                helper.absolutePos(new BlockPos(x0 + 5, 2, 18)));
        VormirSites.add(site);
        f.onClose(() -> VormirSites.remove(site));
        return new VormirTestSite(helper, x0, site);
    }

    /** The first half of the arena. */
    public static VormirTestSite build(TestKit.Fixture f) {
        return build(f, 0);
    }

    private static BoundingBox box(GameTestHelper helper, int x1, int y1, int z1, int x2, int y2, int z2) {
        return BoundingBox.fromCorners(helper.absolutePos(new BlockPos(x1, y1, z1)),
                helper.absolutePos(new BlockPos(x2, y2, z2)));
    }

    public VormirSite site() {
        return site;
    }

    /** A place on the edge (relative x {@code x0 + 2 + dx}). */
    public Vec3 edge(int dx) {
        return helper.absoluteVec(new Vec3(x0 + 2.5 + dx, 6.0, 10.5));
    }

    /** The creature stands on the edge (on the ground). */
    public void standOnEdge(LivingEntity entity, int dx) {
        Vec3 at = edge(dx);
        entity.teleportTo(at.x, at.y, at.z);
        entity.setOnGround(true);
        entity.setDeltaMovement(Vec3.ZERO);
    }

    /** The creature is in the air over the chasm (as if pushed off), {@code up} blocks over its floor. */
    public void overThePit(LivingEntity entity, double up) {
        Vec3 at = helper.absoluteVec(new Vec3(x0 + 5.5, 2.0 + up, 5.5));
        entity.teleportTo(at.x, at.y, at.z);
        entity.setOnGround(false);
    }

    /** The middle of the pool's place (where the seeker wakes). */
    public Vec3 pool() {
        return Vec3.atBottomCenterOf(site.pool());
    }

    /** The creature lands from the chasm's height (40 blocks) and dies of it (unless something saves it). */
    public static boolean fallDie(LivingEntity entity) {
        entity.fallDistance = 40.0F;
        return entity.hurt(entity.damageSources().fall(), 1000.0F);
    }

    /** The items lying in the site. */
    public List<ItemEntity> items() {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, AABB.of(site.bounds()).inflate(2.0));
    }

    /** The Stonekeeper's lines (lang keys) the player was told since the last look, in order. */
    public static List<String> keeperLines(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        for (Object packet : TestKit.sentPackets(player)) {
            if (packet instanceof ClientboundSystemChatPacket chat && !chat.overlay()
                    && chat.content().getContents() instanceof TranslatableContents said
                    && said.getKey().equals("npc.enhanced.stonekeeper.say") && said.getArgs().length == 2
                    && said.getArgs()[1] instanceof Component line
                    && line.getContents() instanceof TranslatableContents key) {
                lines.add(key.getKey());
            }
        }
        return lines;
    }

    /** Whether the tracker saw the creature leave this edge (and holds a departure for it). */
    public boolean departed(LivingEntity entity) {
        VormirTracker.Departure gone = VormirTracker.departure(entity);
        return gone != null && gone.site().equals(site);
    }
}

package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.CrawlMath;
import com.kolodkingal.enhanced.client.render.LegIk;
import com.kolodkingal.enhanced.client.render.WebTiming;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Spider-Man's client side (DESIGN section 14): what can be tested without a client, which is the pure arithmetic of
 * the Iron Spider's legs ({@link LegIk}), of the cycle of his crawl ({@link CrawlMath}) and of how long his web effects
 * last ({@link WebTiming}), and the art the client draws with. The movement, the effects and the layers themselves are
 * checked in a real game (see the notes of the stage). A test class has no client class in a signature, so nothing here
 * names one.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class SpiderClientTests {
    private static final String QUICK = "spider_client";

    // ------------------------------------------------------------------------------------------------------------
    // The legs

    private static final double L1 = 9.0;
    private static final double L2 = 10.0;
    private static final double L3 = 7.0;
    /**
     * The length of the second and third segments taken as one bone (the third is held at 30 degrees to the second).
     */
    private static final double L23 = Math.sqrt(L2 * L2 + L3 * L3 + 2.0 * L2 * L3 * Math.cos(LegIk.TIP_ANGLE));

    /**
     * For random targets within reach (not nearer than the leg can fold to) the tip is on the target, whatever the
     * knee's hint; the segments keep their lengths.
     */
    @GameTest(template = ARENA, batch = QUICK)
    public static void legIkReachesItsTarget(GameTestHelper helper) {
        RandomSource random = RandomSource.create(20260611L);
        Vec3 root = new Vec3(3, 3, 2.5);
        for (int i = 0; i < 400; i++) {
            double fold = Math.abs(L1 - L23);
            double distance = fold + 0.1 + random.nextDouble() * (L1 + L23 - fold - 0.2);
            Vec3 direction = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).normalize();
            Vec3 target = root.add(direction.scale(distance));
            Vec3 bend = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 1.0, random.nextDouble() - 0.5);
            LegIk.Pose pose = LegIk.solve(root, target, bend, L1, L2, L3);
            check(pose.tip().distanceTo(target) < 1.0E-3, "the tip is on the target " + i + ": off by " + pose.tip().distanceTo(target));
            assertNear(pose.knee().distanceTo(root), L1, 1.0E-6, "the first segment keeps its length");
            assertNear(pose.ankle().distanceTo(pose.knee()), L2, 1.0E-6, "the second segment keeps its length");
            assertNear(pose.tip().distanceTo(pose.ankle()), L3, 1.0E-6, "the third segment keeps its length");
            assertTipAngle(pose);
        }
        helper.succeed();
    }

    /** The third segment is held at 30 degrees to the second, whatever the pose. */
    private static void assertTipAngle(LegIk.Pose pose) {
        Vec3 second = pose.ankle().subtract(pose.knee()).normalize();
        Vec3 third = pose.tip().subtract(pose.ankle()).normalize();
        assertNear(Math.toDegrees(Math.acos(Math.min(1.0, Math.max(-1.0, second.dot(third))))), 30.0, 1.0E-4, "the third segment is at 30 degrees to the second");
    }

    /**
     * Out of reach the leg is stretched and points at the target; too near it folds and never breaks into numbers that
     * are not numbers.
     */
    @GameTest(template = ARENA, batch = QUICK)
    public static void legIkStretchesAndFolds(GameTestHelper helper) {
        Vec3 root = new Vec3(0, 0, 0);
        Vec3 bend = new Vec3(0, -1, 0.3);
        Vec3 far = new Vec3(0, 0, -60);
        LegIk.Pose stretched = LegIk.solve(root, far, bend, L1, L2, L3);
        assertNear(stretched.tip().distanceTo(root), L1 + L23, 1.0E-6, "a leg that cannot reach is as long as it goes");
        // Its first segment points along the way to the target, and so does the chord from the knee to the tip.
        Vec3 chord = stretched.tip().subtract(root).normalize();
        assertNear(chord.dot(far.normalize()), 1.0, 1.0E-9, "a leg that cannot reach points at it");
        assertNear(stretched.knee().subtract(root).normalize().dot(far.normalize()), 1.0, 1.0E-9, "with its first segment straight at it too");
        assertTipAngle(stretched);

        for (Vec3 near : new Vec3[]{root, new Vec3(0.3, 0, 0), new Vec3(1, 1, 1), new Vec3(0, -2, 0.5)}) {
            LegIk.Pose folded = LegIk.solve(root, near, bend, L1, L2, L3);
            check(Double.isFinite(folded.knee().x) && Double.isFinite(folded.ankle().y) && Double.isFinite(folded.tip().z), "a target " + near + " is folded to, not NaN");
            assertNear(folded.knee().distanceTo(root), L1, 1.0E-6, "a folded leg keeps its first segment");
            assertNear(folded.tip().distanceTo(folded.ankle()), L3, 1.0E-6, "and its tip");
            // It folds as near as its two bones can go: a hair off the difference of their lengths.
            check(folded.tip().distanceTo(root) < Math.abs(L1 - L23) + 1.0E-3 + near.length(), "folded " + near + " the tip is near the root");
        }
        // The knee bulges the way the hint says: a hint up puts the knee above the line to the target.
        LegIk.Pose up = LegIk.solve(root, new Vec3(14, 0, 0), new Vec3(0, -1, 0), L1, L2, L3);
        LegIk.Pose down = LegIk.solve(root, new Vec3(14, 0, 0), new Vec3(0, 1, 0), L1, L2, L3);
        check(up.knee().y < -1.0 && down.knee().y > 1.0, "the knee bulges the way the hint says (up is negative y of the model)");
        // A hint along the way to the target must not make it vanish.
        LegIk.Pose along = LegIk.solve(root, new Vec3(14, 0, 0), new Vec3(1, 0, 0), L1, L2, L3);
        check(along.tip().distanceTo(new Vec3(14, 0, 0)) < 1.0E-3, "a hint along the way still reaches");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The crawl

    /** One cycle of his hands and knees for every 1.1 blocks he climbs, and none while he hangs still. */
    @GameTest(template = ARENA, batch = QUICK)
    public static void theCrawlPhaseAdvancesWithDistance(GameTestHelper helper) {
        for (float speed : new float[]{0.05F, 0.1F, 0.22F, 0.4F}) {
            float omega = CrawlMath.omega(speed);
            // Climbing 1.1 blocks takes 1.1 / speed ticks and turns the phase by one whole cycle.
            assertNear(omega * (CrawlMath.CYCLE / speed), 2.0 * Math.PI, 1.0E-4, "one cycle in 1.1 blocks at " + speed);
        }
        assertNear(CrawlMath.omega(0.0F), 0.0, 0.0, "it holds at rest");
        check(CrawlMath.amp(0.0F) == 0.0F && CrawlMath.amp(0.01F) == 0.0F, "no swing at rest");
        check(CrawlMath.amp(0.15F) == 1.0F && CrawlMath.amp(0.5F) == 1.0F, "the whole swing from 0.15 blocks a tick");
        check(CrawlMath.amp(0.08F) > 0.0F && CrawlMath.amp(0.08F) < 1.0F, "a part of it between");
        float phase = 0.0F;
        for (int i = 0; i < 100; i++) {
            float next = CrawlMath.advance(phase, CrawlMath.omega(0.22F), 1.0F);
            check(next > phase, "the phase keeps advancing");
            phase = next;
        }
        assertNear(phase, CrawlMath.omega(0.22F) * 100, 1.0E-3, "100 ticks at the normal crawl speed");
        check(CrawlMath.advance(1.0E4F + 5.0F, 1.0F, 1.0F) < 100.0F, "a long climb wraps the phase instead of losing its precision");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The web effects

    /**
     * The strand of a Web Shot grows, holds and fades in four ticks; a burst's in seven; a splat fades over its last
     * eight.
     */
    @GameTest(template = ARENA, batch = QUICK)
    public static void theWebEffectsRunTheirCourse(GameTestHelper helper) {
        check(WebTiming.shotReach(0.0F) == 0.0F && WebTiming.shotReach(WebTiming.SHOT_GROW) == 1.0F && WebTiming.shotReach(3.0F) == 1.0F, "a shot grows over two ticks");
        check(WebTiming.shotAlpha(0.0F) == 1.0F && WebTiming.shotAlpha(2.9F) == 1.0F, "it holds");
        check(WebTiming.shotAlpha(3.5F) > 0.0F && WebTiming.shotAlpha(3.5F) < 1.0F && WebTiming.shotAlpha(WebTiming.SHOT_TICKS) == 0.0F, "and fades by the fourth tick");
        check(WebTiming.shotAlpha(-1.0F) == 0.0F, "nothing before it began");

        check(WebTiming.burstReach(1.0F) == 0.5F && WebTiming.burstReach(2.0F) == 1.0F, "a burst's strands go out in two ticks");
        check(WebTiming.burstAlpha(3.9F) == 1.0F, "hold two");
        check(WebTiming.burstAlpha(5.5F) > 0.0F && WebTiming.burstAlpha(5.5F) < 1.0F && WebTiming.burstAlpha(WebTiming.BURST_TICKS) == 0.0F, "fade over three");

        check(WebTiming.splatAlpha(0.0F, WebTiming.SPLAT_TICKS) == 1.0F && WebTiming.splatAlpha(WebTiming.SPLAT_TICKS - WebTiming.SPLAT_FADE, WebTiming.SPLAT_TICKS) == 1.0F,
                "a splat is whole until its last eight ticks");
        assertNear(WebTiming.splatAlpha(WebTiming.SPLAT_TICKS - 4.0F, WebTiming.SPLAT_TICKS), 0.5, 1.0E-6, "half-way through the fade");
        check(WebTiming.splatAlpha(WebTiming.SPLAT_TICKS, WebTiming.SPLAT_TICKS) == 0.0F, "gone at its end");
        check(WebTiming.splatAlpha(WebTiming.BURST_SPLAT_TICKS - 1.0F, WebTiming.BURST_SPLAT_TICKS) > 0.0F, "a burst's splat lasts 14 ticks");

        check(WebTiming.releaseFly(0.0F) == 0.0F && WebTiming.releaseFly(WebTiming.RELEASE_FLY) == 1.0F, "a line that lets go flies to its anchor in four ticks");
        check(WebTiming.releaseAlpha(0.0F) == 1.0F && WebTiming.releaseAlpha(WebTiming.RELEASE_FADE) == 0.0F, "and fades over five");

        // The shimmer of a slack line dies away as it goes taut, and is nil at both of its ends.
        float slack = 0.0F;
        float taut = 0.0F;
        for (int i = 0; i < 50; i++) {
            slack = Math.max(slack, Math.abs(WebTiming.ripple(i * 0.7F, 3.0F, 0.0F, 0.5F)));
            taut = Math.max(taut, Math.abs(WebTiming.ripple(i * 0.7F, 3.0F, 40.0F, 0.5F)));
        }
        check(slack > 0.01F && slack <= 0.0201F, "a slack line ripples by up to two centimetres: " + slack);
        check(taut == 0.0F, "a taut one does not");
        assertNear(WebTiming.ripple(5.0F, 3.0F, 0.0F, 0.0F), 0.0, 1.0E-6, "it does not move at its hand");
        assertNear(WebTiming.ripple(5.0F, 3.0F, 0.0F, 1.0F), 0.0, 1.0E-5, "nor at its anchor");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The art the client draws with

    /** The eye overlays only paint the lenses, and the legs and the splat are the size their layers read them at. */
    @GameTest(template = ARENA, batch = QUICK)
    public static void theClientTexturesAreThere(GameTestHelper helper) {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        for (String eyes : new String[]{"black_homemade", "black_iron", "black_bnd", "black_own", "kill_iron"}) {
            BufferedImage image = read(mod.findResource("assets", Enhanced.MODID, "textures", "entity", "spider_eyes_" + eyes + ".png"));
            check(image.getWidth() == 64 && image.getHeight() == 64, "spider_eyes_" + eyes + " is 64 x 64");
            int painted = 0;
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    int alpha = image.getRGB(x, y) >>> 24;
                    check(alpha == 0 || alpha == 255, eyes + ": every pixel is opaque or clear");
                    if (alpha != 0) {
                        painted++;
                        // Only the front of the face (8, 8)-(16, 16) is painted: the lenses of the suit.
                        check(x >= 8 && x < 16 && y >= 8 && y < 16, eyes + ": pixel " + x + "," + y + " is outside the front of the face");
                    }
                }
            }
            check(painted >= 4 && painted <= 24, eyes + " paints a few pixels: " + painted);
        }
        BufferedImage legs = read(mod.findResource("assets", Enhanced.MODID, "textures", "entity", "spider_legs.png"));
        check(legs.getWidth() == 32 && legs.getHeight() == 32, "spider_legs is 32 x 32");
        BufferedImage splat = read(mod.findResource("assets", Enhanced.MODID, "textures", "effect", "web_splat.png"));
        check(splat.getWidth() == 32 && splat.getHeight() == 32, "web_splat is 32 x 32");
        // The splat is a web: clear at the corners, white at the middle.
        check((splat.getRGB(0, 0) >>> 24) == 0 && (splat.getRGB(31, 31) >>> 24) == 0, "the splat's corners are clear");
        check((splat.getRGB(16, 16) >>> 24) > 200, "and its middle is solid");
        helper.succeed();
    }

    private static BufferedImage read(Path path) {
        check(Files.isRegularFile(path), path + " exists");
        try (InputStream in = Files.newInputStream(path)) {
            BufferedImage image = ImageIO.read(in);
            check(image != null, path + " is a PNG");
            return image;
        } catch (IOException e) {
            throw new AssertionError(path + " cannot be read", e);
        }
    }
}

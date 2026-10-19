package com.kolodkingal.enhanced.world;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/**
 * The fixed points of the stones' four sites (3.6, the plan's section 4), template-relative and unturned (x east, z
 * south, y the template's layer). The 3.6 skeleton writes them from the plan (and its own choices where the plan named
 * a point without a place: see each); lane L4 builds the templates to them and tests them, lanes L1 and L5 read them. A
 * point that moves is moved here, by lane L4, with a note.
 */
public final class StoneSitePlans {

    /** The Sanctum Sanctorum (the Time Stone): a three-storey brownstone with a cellar of three layers. */
    public static final class Sanctum {
        public static final ResourceLocation TEMPLATE = Enhanced.id("sanctum/house");
        public static final Vec3i SIZE = new Vec3i(23, 26, 19);
        /** The layer at ground level (the three below are the ward room). */
        public static final int FLOOR = 3;
        /** The stone pedestal with the Time Stone, on the top floor under the Window of Worlds. */
        public static final BlockPos PEDESTAL = new BlockPos(11, FLOOR + 13, 13);

        private Sanctum() {
        }
    }

    /** Avengers Tower (the Tesseract): three templates stacked, the base 25 wide, the mid and top 21 wide, centred. */
    public static final class Tower {
        public static final ResourceLocation BASE = Enhanced.id("avengers_tower/base");
        public static final ResourceLocation MID = Enhanced.id("avengers_tower/mid");
        public static final ResourceLocation TOP = Enhanced.id("avengers_tower/top");
        public static final List<ResourceLocation> PARTS = List.of(BASE, MID, TOP);
        public static final Vec3i BASE_SIZE = new Vec3i(25, 32, 25);
        public static final Vec3i MID_SIZE = new Vec3i(21, 32, 21);
        public static final Vec3i TOP_SIZE = new Vec3i(21, 28, 21);
        /** The y of each part above the base's layer 0 (the tower is 92 tall). */
        public static final int[] PART_Y = {0, 32, 64};
        /** The layers of the base below the ground (the garage). */
        public static final int FLOOR = 2;
        /**
         * The Tesseract's cradle on the roof (Selvig's device), in the {@code top} template (the skeleton's choice: the
         * roof's middle; the plan names the point, not its place).
         */
        public static final BlockPos CRADLE_IN_TOP = new BlockPos(10, 24, 10);
        /**
         * The cradle in the whole tower's frame (the base's): the top part is centred on the base, 2 in from its edges.
         */
        public static final BlockPos CRADLE = CRADLE_IN_TOP.offset((BASE_SIZE.getX() - TOP_SIZE.getX()) / 2, PART_Y[2],
                (BASE_SIZE.getZ() - TOP_SIZE.getZ()) / 2);

        private Tower() {
        }
    }

    /** The End tunnel (the Power Stone in the Orb): an entrance, a winding tunnel and the vault at its end. */
    public static final class EndTunnel {
        public static final ResourceLocation TEMPLATE = Enhanced.id("end_tunnel/vault");
        public static final Vec3i SIZE = new Vec3i(15, 14, 46);
        /** The vault (11 x 7 x 11) at the tunnel's end (the skeleton's choice: the last 11 layers of z). */
        public static final BoundingBox VAULT = new BoundingBox(2, 0, 35, 12, 6, 45);
        /** The vault chest on its dais (the skeleton's choice: the vault's middle, on a dais of one). */
        public static final BlockPos VAULT_CHEST = new BlockPos(7, 2, 40);

        private EndTunnel() {
        }
    }

    /** The Vormir Cliff (the Soul Stone): a pinnacle with a chasm down its middle, the summit round its top. */
    public static final class Vormir {
        public static final ResourceLocation TEMPLATE = Enhanced.id("vormir/cliff");
        public static final Vec3i SIZE = new Vec3i(33, 74, 33);
        /** The middle of the summit (layer 66), over the chasm. */
        public static final BlockPos SUMMIT = new BlockPos(16, 66, 16);
        /**
         * The strip at the chasm's lip where a fall must begin: 9 x 2 x 2, on the summit's south side, between the
         * chasm and the pool (the skeleton's reading of the plan's "the 9 x 2 x 2 strip on the summit at the chasm's
         * lip").
         */
        public static final BoundingBox EDGE_BOX = new BoundingBox(12, 66, 21, 20, 67, 22);
        /** The shaft of the chasm, from just over the pit's floor up through the pinnacle. */
        public static final BoundingBox PIT_BOX = new BoundingBox(12, 3, 12, 20, 62, 20);
        /** The layer of the pit's floor. */
        public static final int PIT_FLOOR = 4;
        /** The shallow pool where the stone is given. */
        public static final BlockPos POOL = new BlockPos(16, 66, 25);
        /** Where the Stonekeeper stands. */
        public static final BlockPos STONEKEEPER_HOME = new BlockPos(16, 67, 9);

        private Vormir() {
        }
    }

    private StoneSitePlans() {
    }
}

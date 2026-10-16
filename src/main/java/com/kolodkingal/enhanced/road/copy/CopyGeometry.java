package com.kolodkingal.enhanced.road.copy;

/**
 * Where everything of the Copy the Structure trial stands, as offsets from the middle of its cell ({@code x} east, {@code y}
 * up, {@code z} south). The model and the plot have the same shape and the same way round and stand side by side, three blocks
 * apart, so the player can build by looking from one to the other: the model on the west, the plot on the east, their fronts
 * (the greatest {@code z}) to the south where he stands. The floor is at {@code y = 0}; the first layer of a structure is at
 * {@code y = 1}. Plain numbers, so that the drawing, the rules and the tests all agree.
 *
 * @param w the width (x) of the model and the plot
 * @param h their height (y)
 * @param d their depth (z)
 */
public record CopyGeometry(int w, int h, int d) {
    /** Blocks of empty floor between the model and the plot. */
    public static final int GAP = 3;

    public static CopyGeometry of(StructureModel model) {
        return new CopyGeometry(model.sizeX(), model.sizeY(), model.sizeZ());
    }

    /** The west edge of the model. */
    public int modelX0() {
        return -w - 1;
    }

    /** The west edge of the plot. */
    public int plotX0() {
        return modelX0() + w + GAP;
    }

    /** The layers the player may build in: the model's height and the room for scaffolding above. */
    public int plotHeight() {
        return h + StructureCompare.EXTRA_HEIGHT;
    }

    /** Whether an offset from the cell's middle is inside the plot the player may build in. */
    public boolean inPlot(int dx, int dy, int dz) {
        return dx >= plotX0() && dx < plotX0() + w && dy >= 1 && dy <= plotHeight() && dz >= 0 && dz < d;
    }

    // ---- the big platform ----

    public int floorMinX() {
        return -w - 6;
    }

    public int floorMaxX() {
        return w + 6;
    }

    public int floorMinZ() {
        return -5;
    }

    public int floorMaxZ() {
        return d + 13;
    }

    /** The middle of the start platform (its z; its x is 0). */
    public int startZ() {
        return d + 10;
    }

    /** The row the console (supply chest, Check and Hint buttons) stands on: in front of the aisle between the model and the plot. */
    public int consoleZ() {
        return d + 2;
    }

    /** The middle of the gate at the far end of the aisle. */
    public int gateZ() {
        return -3;
    }
}

package model;

/**
 * Obdélníkové ořezávací okno s hranicemi rovnoběžnými s osami X a Y.
 */
public class ClipRect {

    private final int xMin;
    private final int yMin;
    private final int xMax;
    private final int yMax;

    public ClipRect(int xMin, int yMin, int xMax, int yMax) {
        this.xMin = Math.min(xMin, xMax);
        this.yMin = Math.min(yMin, yMax);
        this.xMax = Math.max(xMin, xMax);
        this.yMax = Math.max(yMin, yMax);
    }

    public int getXMin() {
        return xMin;
    }

    public int getYMin() {
        return yMin;
    }

    public int getXMax() {
        return xMax;
    }

    public int getYMax() {
        return yMax;
    }
}

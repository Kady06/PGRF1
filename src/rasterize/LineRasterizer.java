package rasterize;

import model.Line;
import raster.Raster;

public abstract class LineRasterizer {

    protected final Raster raster;
    protected int color = 0xffff00;

    public LineRasterizer(Raster raster) {
        this.raster = raster;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void rasterize(Line line) {
        setColor(line.getColor());
        rasterize(line.getX1(), line.getY1(), line.getX2(), line.getY2());
    }

    public void rasterize(int x1, int y1, int x2, int y2, int color) {
        setColor(color);
        rasterize(x1, y1, x2, y2);
    }

    public abstract void rasterize(int x1, int y1, int x2, int y2);
}

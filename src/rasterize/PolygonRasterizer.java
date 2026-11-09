package rasterize;

import model.Point;
import model.Polygon;

public class PolygonRasterizer {

    private final LineRasterizer lineRasterizer;

    public PolygonRasterizer(LineRasterizer lineRasterizer) {
        this.lineRasterizer = lineRasterizer;
    }

    public void rasterize(Polygon polygon) {
        if (polygon.getSize() < 2) {
            return;
        }

        int size = polygon.getSize();
        for (int i = 0; i < size; i++) {
            Point a = polygon.getPoint(i);
            Point b = polygon.getPoint((i + 1) % size);

            // U 2 vrcholů jen jedna úsečka; od 3 vrcholů uzavíráme (poslední → první).
            if (size == 2 && i == 1) {
                break;
            }

            lineRasterizer.rasterize(a.getX(), a.getY(), b.getX(), b.getY());
        }
    }

    public void rasterize(Polygon polygon, int color) {
        lineRasterizer.setColor(color);
        rasterize(polygon);
    }
}

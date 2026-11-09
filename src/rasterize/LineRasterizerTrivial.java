package rasterize;

import raster.Raster;

/**
 * Triviální rasterizace úsečky (y = k*x + q), rozšířená o správný směr,
 */
public class LineRasterizerTrivial extends LineRasterizer {

    public LineRasterizerTrivial(Raster raster) {
        super(raster);
    }

    @Override
    public void rasterize(int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;

        if (dx == 0 && dy == 0) {
            raster.setPixel(x1, y1, color);
            return;
        }

        if (Math.abs(dx) >= Math.abs(dy)) {
            // Dominantní osa X (včetně vodorovných a |k| ≤ 1)
            if (x1 > x2) {
                int tmpX = x1;
                int tmpY = y1;
                x1 = x2;
                y1 = y2;
                x2 = tmpX;
                y2 = tmpY;
            }

            float k = (y2 - y1) / (float) (x2 - x1);
            float q = y1 - k * x1;

            for (int x = x1; x <= x2; x++) {
                int y = Math.round(k * x + q);
                raster.setPixel(x, y, color);
            }
        } else {
            // Dominantní osa Y (strmé a svislé úsečky)
            if (y1 > y2) {
                int tmpX = x1;
                int tmpY = y1;
                x1 = x2;
                y1 = y2;
                x2 = tmpX;
                y2 = tmpY;
            }

            float kInv = (x2 - x1) / (float) (y2 - y1);

            for (int y = y1; y <= y2; y++) {
                int x = Math.round(x1 + kInv * (y - y1));
                raster.setPixel(x, y, color);
            }
        }
    }
}

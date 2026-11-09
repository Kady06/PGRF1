package rasterize;

import model.ClipRect;
import model.Line;
import model.Point;

/**
 * Cohen–Sutherland ořezání úsečky obdélníkovým oknem.
 * Používá 4bitové kódy polohy; řeší triviální přijetí, triviální zamítnutí
 * i výpočet průsečíků s hranicemi okna.
 */
public final class CohenSutherland {

    private static final int INSIDE = 0;
    private static final int LEFT = 1;
    private static final int RIGHT = 2;
    private static final int BELOW = 4; // y > yMax (na obrazovce dolů)
    private static final int ABOVE = 8; // y < yMin (na obrazovce nahoru)

    private CohenSutherland() {
    }

    /**
     * @return nová úsečka uvnitř okna, nebo {@code null} při triviálním zamítnutí
     */
    public static Line clip(Line line, ClipRect rect) {
        if (line == null || rect == null) {
            return null;
        }

        double x0 = line.getX1();
        double y0 = line.getY1();
        double x1 = line.getX2();
        double y1 = line.getY2();

        int code0 = outCode(x0, y0, rect);
        int code1 = outCode(x1, y1, rect);

        while (true) {
            // Triviální přijetí – oba body uvnitř
            if ((code0 | code1) == INSIDE) {
                return new Line(
                        new Point((int) Math.round(x0), (int) Math.round(y0)),
                        new Point((int) Math.round(x1), (int) Math.round(y1)),
                        line.getColor()
                );
            }

            // Triviální zamítnutí – oba body ve stejném vnějším regionu
            if ((code0 & code1) != INSIDE) {
                return null;
            }

            // Bod mimo okno – spočítáme průsečík s jednou hranicí
            int codeOut = code0 != INSIDE ? code0 : code1;
            double x;
            double y;

            if ((codeOut & ABOVE) != 0) {
                x = x0 + (x1 - x0) * (rect.getYMin() - y0) / (y1 - y0);
                y = rect.getYMin();
            } else if ((codeOut & BELOW) != 0) {
                x = x0 + (x1 - x0) * (rect.getYMax() - y0) / (y1 - y0);
                y = rect.getYMax();
            } else if ((codeOut & RIGHT) != 0) {
                y = y0 + (y1 - y0) * (rect.getXMax() - x0) / (x1 - x0);
                x = rect.getXMax();
            } else { // LEFT
                y = y0 + (y1 - y0) * (rect.getXMin() - x0) / (x1 - x0);
                x = rect.getXMin();
            }

            if (codeOut == code0) {
                x0 = x;
                y0 = y;
                code0 = outCode(x0, y0, rect);
            } else {
                x1 = x;
                y1 = y;
                code1 = outCode(x1, y1, rect);
            }
        }
    }

    private static int outCode(double x, double y, ClipRect rect) {
        int code = INSIDE;
        if (x < rect.getXMin()) {
            code |= LEFT;
        } else if (x > rect.getXMax()) {
            code |= RIGHT;
        }
        if (y < rect.getYMin()) {
            code |= ABOVE;
        } else if (y > rect.getYMax()) {
            code |= BELOW;
        }
        return code;
    }
}

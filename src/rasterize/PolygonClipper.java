package rasterize;

import model.Point;
import model.Polygon;

import java.util.ArrayList;
import java.util.List;

/**
 * Sutherland–Hodgman: ořezání libovolného uzavřeného n-úhelníku konvexním polygonem.
 * Orientace ořezávacího polygonu (CW/CCW) se detekuje ze znaménka plochy.
 */
public final class PolygonClipper {

    private PolygonClipper() {
    }

    public static Polygon clip(Polygon subject, Polygon clip) {
        if (subject == null || clip == null || subject.getSize() < 3 || clip.getSize() < 3) {
            return new Polygon();
        }

        boolean clipCcw = signedArea(clip) > 0;
        List<double[]> output = toVertices(subject);

        int clipSize = clip.getSize();
        for (int i = 0; i < clipSize; i++) {
            Point a = clip.getPoint(i);
            Point b = clip.getPoint((i + 1) % clipSize);

            List<double[]> input = output;
            output = new ArrayList<>();
            if (input.isEmpty()) {
                break;
            }

            double[] s = input.get(input.size() - 1);
            for (double[] e : input) {
                boolean eInside = isInside(e, a, b, clipCcw);
                boolean sInside = isInside(s, a, b, clipCcw);

                if (eInside) {
                    if (!sInside) {
                        output.add(intersection(s, e, a, b));
                    }
                    output.add(e);
                } else if (sInside) {
                    output.add(intersection(s, e, a, b));
                }
                s = e;
            }
        }

        return toPolygon(output);
    }

    /** Obrátí pořadí vrcholů (změna orientace CW ↔ CCW). */
    public static void reverseOrientation(Polygon polygon) {
        List<Point> pts = polygon.getPoints();
        int i = 0;
        int j = pts.size() - 1;
        while (i < j) {
            Point tmp = pts.get(i);
            pts.set(i, pts.get(j));
            pts.set(j, tmp);
            i++;
            j--;
        }
    }

    private static double signedArea(Polygon polygon) {
        double area = 0;
        int n = polygon.getSize();
        for (int i = 0; i < n; i++) {
            Point p1 = polygon.getPoint(i);
            Point p2 = polygon.getPoint((i + 1) % n);
            area += (double) p1.getX() * p2.getY() - (double) p2.getX() * p1.getY();
        }
        return area / 2.0;
    }

    /**
     * Bod je uvnitř vůči orientované hraně clipu A→B.
     * CCW clip: vnitřek vlevo (cross ≥ 0), CW clip: vnitřek vpravo (cross ≤ 0).
     */
    private static boolean isInside(double[] p, Point a, Point b, boolean clipCcw) {
        double cross = (b.getX() - a.getX()) * (p[1] - a.getY())
                - (b.getY() - a.getY()) * (p[0] - a.getX());
        return clipCcw ? cross >= 0 : cross <= 0;
    }

    private static double[] intersection(double[] s, double[] e, Point a, Point b) {
        double x1 = s[0];
        double y1 = s[1];
        double x2 = e[0];
        double y2 = e[1];
        double x3 = a.getX();
        double y3 = a.getY();
        double x4 = b.getX();
        double y4 = b.getY();

        double den = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(den) < 1e-12) {
            return new double[]{x2, y2};
        }

        double t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / den;
        return new double[]{
                x1 + t * (x2 - x1),
                y1 + t * (y2 - y1)
        };
    }

    private static List<double[]> toVertices(Polygon polygon) {
        List<double[]> vertices = new ArrayList<>(polygon.getSize());
        for (int i = 0; i < polygon.getSize(); i++) {
            Point p = polygon.getPoint(i);
            vertices.add(new double[]{p.getX(), p.getY()});
        }
        return vertices;
    }

    private static Polygon toPolygon(List<double[]> vertices) {
        Polygon result = new Polygon();
        for (double[] v : vertices) {
            result.addPoint(new Point(
                    (int) Math.round(v[0]),
                    (int) Math.round(v[1])
            ));
        }
        // Odstranění případných po sobě jdoucích duplicit po zaokrouhlení.
        removeDuplicateConsecutive(result);
        return result;
    }

    private static void removeDuplicateConsecutive(Polygon polygon) {
        List<Point> pts = polygon.getPoints();
        if (pts.size() < 2) {
            return;
        }
        for (int i = pts.size() - 1; i > 0; i--) {
            Point a = pts.get(i);
            Point b = pts.get(i - 1);
            if (a.getX() == b.getX() && a.getY() == b.getY()) {
                pts.remove(i);
            }
        }
        if (pts.size() >= 2) {
            Point first = pts.get(0);
            Point last = pts.get(pts.size() - 1);
            if (first.getX() == last.getX() && first.getY() == last.getY()) {
                pts.remove(pts.size() - 1);
            }
        }
    }
}

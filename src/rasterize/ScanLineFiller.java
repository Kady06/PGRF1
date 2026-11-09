package rasterize;

import model.Point;
import model.Polygon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Scan-line vyplnění geometricky zadaného n-úhelníku.
 * Pro každý řádek y najde průsečíky s nehorizontálními hranami,
 * seřadí je a spojuje lichý se sudým.
 * Volitelně ořezává pixely překryté později kreslenými n-úhelníky.
 */
public class ScanLineFiller {

    private final LineRasterizer lineRasterizer;

    public ScanLineFiller(LineRasterizer lineRasterizer) {
        this.lineRasterizer = lineRasterizer;
    }

    public void fill(Polygon polygon, int fillColor) {
        fill(polygon, fillColor, List.of());
    }

    public void fill(Polygon polygon, int fillColor, List<Polygon> occluders) {
        if (polygon.getSize() < 3) {
            return;
        }

        List<Edge> edges = buildEdges(polygon);
        if (edges.isEmpty()) {
            return;
        }

        int yMin = polygon.getPoint(0).getY();
        int yMax = yMin;
        for (int i = 1; i < polygon.getSize(); i++) {
            int y = polygon.getPoint(i).getY();
            yMin = Math.min(yMin, y);
            yMax = Math.max(yMax, y);
        }

        List<Integer> intersections = new ArrayList<>();
        for (int y = yMin; y <= yMax; y++) {
            intersections.clear();
            for (Edge edge : edges) {
                if (edge.hasIntersection(y)) {
                    intersections.add(edge.intersectX(y));
                }
            }

            Collections.sort(intersections);

            for (int i = 0; i + 1 < intersections.size(); i += 2) {
                int x1 = intersections.get(i);
                int x2 = intersections.get(i + 1);
                drawSpan(x1, x2, y, fillColor, occluders);
            }
        }
    }

    private void drawSpan(int x1, int x2, int y, int fillColor, List<Polygon> occluders) {
        if (x1 > x2) {
            int tmp = x1;
            x1 = x2;
            x2 = tmp;
        }

        if (occluders == null || occluders.isEmpty()) {
            lineRasterizer.rasterize(x1, y, x2, y, fillColor);
            return;
        }

        int x = x1;
        while (x <= x2) {
            while (x <= x2 && isOccluded(x, y, occluders)) {
                x++;
            }
            if (x > x2) {
                break;
            }
            int runStart = x;
            while (x <= x2 && !isOccluded(x, y, occluders)) {
                x++;
            }
            lineRasterizer.rasterize(runStart, y, x - 1, y, fillColor);
        }
    }

    private static boolean isOccluded(int x, int y, List<Polygon> occluders) {
        for (Polygon occluder : occluders) {
            if (occluder.getSize() >= 3 && occluder.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    private List<Edge> buildEdges(Polygon polygon) {
        List<Edge> edges = new ArrayList<>();
        int size = polygon.getSize();

        for (int i = 0; i < size; i++) {
            Point a = polygon.getPoint(i);
            Point b = polygon.getPoint((i + 1) % size);
            Edge edge = new Edge(a.getX(), a.getY(), b.getX(), b.getY());
            if (edge.isHorizontal()) {
                continue;
            }
            edge.orientate();
            edges.add(edge);
        }
        return edges;
    }

    /**
     * Orientovaná hrana (y1 &lt; y2). Průsečík se počítá pro y ∈ [y1, y2),
     * čímž se horní vrchol nezapočítá dvakrát.
     */
    private static final class Edge {
        private int x1;
        private int y1;
        private int x2;
        private int y2;

        Edge(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        boolean isHorizontal() {
            return y1 == y2;
        }

        void orientate() {
            if (y1 > y2) {
                int tmpX = x1;
                int tmpY = y1;
                x1 = x2;
                y1 = y2;
                x2 = tmpX;
                y2 = tmpY;
            }
        }

        boolean hasIntersection(int y) {
            return y >= y1 && y < y2;
        }

        int intersectX(int y) {
            return (int) Math.round(x1 + (x2 - x1) * (y - y1) / (double) (y2 - y1));
        }
    }
}

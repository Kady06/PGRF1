package model;

import java.util.ArrayList;
import java.util.List;

public class Polygon {

    private final List<Point> points;

    public Polygon() {
        points = new ArrayList<>();
    }

    public void addPoint(Point point) {
        points.add(point);
    }

    public void insertPoint(int index, Point point) {
        points.add(index, point);
    }

    public void setPoint(int index, Point point) {
        points.set(index, point);
    }

    public Point getPoint(int index) {
        return points.get(index);
    }

    public Point getFirst() {
        return points.get(0);
    }

    public Point getLast() {
        return points.get(points.size() - 1);
    }

    public int getSize() {
        return points.size();
    }

    public List<Point> getPoints() {
        return points;
    }

    public void clear() {
        points.clear();
    }

    /**
     * Index nejbližšího vrcholu k [x, y], nebo -1 pokud je vzdálenost větší než maxDistance.
     */
    public int findNearestVertex(int x, int y, double maxDistance) {
        int best = -1;
        double bestDist = maxDistance;

        for (int i = 0; i < points.size(); i++) {
            double dist = points.get(i).distanceTo(x, y);
            if (dist <= bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return best;
    }

    /**
     * Index počátečního vrcholu nejbližší hrany (hrana i → (i+1) % n).
     * Pro neuzavřený 2bodový polygon jen hrana 0→1.
     * Vrací -1, pokud není žádná hrana.
     */
    public int findNearestEdge(int x, int y) {
        int size = points.size();
        if (size < 2) {
            return -1;
        }

        int edgeCount = size >= 3 ? size : 1;
        int best = -1;
        double bestDist = Double.MAX_VALUE;

        for (int i = 0; i < edgeCount; i++) {
            Point a = points.get(i);
            Point b = points.get((i + 1) % size);
            double dist = distanceToSegment(x, y, a, b);
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return best;
    }

    /**
     * Vzdálenost bodu od úsečky AB.
     */
    public static double distanceToSegment(int px, int py, Point a, Point b) {
        double ax = a.getX();
        double ay = a.getY();
        double bx = b.getX();
        double by = b.getY();

        double dx = bx - ax;
        double dy = by - ay;

        if (dx == 0 && dy == 0) {
            return a.distanceTo(px, py);
        }

        double t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));

        double projX = ax + t * dx;
        double projY = ay + t * dy;
        double ex = px - projX;
        double ey = py - projY;
        return Math.sqrt(ex * ex + ey * ey);
    }
}

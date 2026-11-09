package model;

public class Line {

    private final Point start;
    private final Point end;
    private final int color;

    public Line(Point start, Point end, int color) {
        this.start = start;
        this.end = end;
        this.color = color;
    }

    public Line(int x1, int y1, int x2, int y2, int color) {
        this(new Point(x1, y1), new Point(x2, y2), color);
    }

    public Point getStart() {
        return start;
    }

    public Point getEnd() {
        return end;
    }

    public int getX1() {
        return start.getX();
    }

    public int getY1() {
        return start.getY();
    }

    public int getX2() {
        return end.getX();
    }

    public int getY2() {
        return end.getY();
    }

    public int getColor() {
        return color;
    }
}

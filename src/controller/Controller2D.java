package controller;

import model.Line;
import model.Point;
import model.Polygon;
import rasterize.LineRasterizer;
import rasterize.LineRasterizerTrivial;
import rasterize.PolygonRasterizer;
import view.Panel;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class Controller2D {

    private static final int COLOR_LINE = 0xffff00;
    private static final int COLOR_POLYGON = 0x00ff00;
    private static final int COLOR_PREVIEW = 0xffffff;
    private static final double VERTEX_PICK_RADIUS = 12.0;

    private enum Mode {
        LINE, POLYGON
    }

    private final Panel panel;
    private final LineRasterizer lineRasterizer;
    private final PolygonRasterizer polygonRasterizer;

    private final List<Line> lines = new ArrayList<>();
    private Polygon polygon = new Polygon();

    private Mode mode = Mode.LINE;


    private boolean drawing;
    private Point startPoint;
    private Point previewPoint;

    private int insertEdgeIndex = -1;


    private boolean editingVertex;
    private int editedVertexIndex = -1;

    public Controller2D(Panel panel) {
        this.panel = panel;
        this.lineRasterizer = new LineRasterizerTrivial(panel.getRaster());
        this.polygonRasterizer = new PolygonRasterizer(lineRasterizer);
        initListeners();
        redraw();
    }

    private void initListeners() {
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    startVertexEdit(e.getX(), e.getY());
                    return;
                }
                if (e.getButton() != MouseEvent.BUTTON1 || editingVertex) {
                    return;
                }

                drawing = true;
                startPoint = new Point(e.getX(), e.getY());
                previewPoint = startPoint;
                updateInsertEdgePreview();
                redraw();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    editingVertex = false;
                    editedVertexIndex = -1;
                    redraw();
                    return;
                }
                if (e.getButton() != MouseEvent.BUTTON1 || !drawing) {
                    return;
                }

                drawing = false;
                Point end = new Point(e.getX(), e.getY());

                if (mode == Mode.LINE) {
                    lines.add(new Line(startPoint, end, COLOR_LINE));
                } else {
                    addPolygonVertex(end);
                }

                startPoint = null;
                previewPoint = null;
                insertEdgeIndex = -1;
                redraw();
            }
        });

        panel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (editingVertex && editedVertexIndex >= 0) {
                    polygon.getPoint(editedVertexIndex).set(e.getX(), e.getY());
                    redraw();
                    return;
                }
                if (!drawing) {
                    return;
                }
                previewPoint = new Point(e.getX(), e.getY());
                updateInsertEdgePreview();
                redraw();
            }
        });

        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_C -> clearAll();
                    case KeyEvent.VK_L -> {
                        cancelInteraction();
                        mode = Mode.LINE;
                        System.out.println("Režim: úsečka (L)");
                        redraw();
                    }
                    case KeyEvent.VK_P -> {
                        cancelInteraction();
                        mode = Mode.POLYGON;
                        System.out.println("Režim: n-úhelník (P)");
                        redraw();
                    }
                    default -> {
                    }
                }
            }
        });
    }

    private void startVertexEdit(int x, int y) {
        if (mode != Mode.POLYGON || polygon.getSize() == 0) {
            return;
        }

        int index = polygon.findNearestVertex(x, y, VERTEX_PICK_RADIUS);
        if (index < 0) {
            return;
        }

        cancelDrawing();
        editingVertex = true;
        editedVertexIndex = index;
        polygon.getPoint(index).set(x, y);
        redraw();
    }

    private void updateInsertEdgePreview() {
        insertEdgeIndex = -1;
        if (mode != Mode.POLYGON || previewPoint == null || polygon.getSize() < 2) {
            return;
        }
        insertEdgeIndex = polygon.findNearestEdge(previewPoint.getX(), previewPoint.getY());
    }

    private void addPolygonVertex(Point point) {
        if (polygon.getSize() == 0) {
            polygon.addPoint(startPoint);
            if (startPoint.getX() != point.getX() || startPoint.getY() != point.getY()) {
                polygon.addPoint(point);
            }
            return;
        }

        if (polygon.getSize() == 1) {
            polygon.addPoint(point);
            return;
        }

        int edgeIndex = polygon.findNearestEdge(point.getX(), point.getY());
        if (edgeIndex < 0) {
            polygon.addPoint(point);
            return;
        }

        polygon.insertPoint(edgeIndex + 1, point);
    }

    private void cancelDrawing() {
        drawing = false;
        startPoint = null;
        previewPoint = null;
        insertEdgeIndex = -1;
    }

    private void cancelInteraction() {
        cancelDrawing();
        editingVertex = false;
        editedVertexIndex = -1;
    }

    private void clearAll() {
        lines.clear();
        polygon = new Polygon();
        cancelInteraction();
        panel.getRaster().clear();
        panel.repaint();
        System.out.println("Plátno a datové struktury vymazány (C)");
    }

    private void redraw() {
        panel.getRaster().clear();
        drawScene();
        drawPreview();
        panel.repaint();
    }

    private void drawScene() {
        for (Line line : lines) {
            lineRasterizer.rasterize(line);
        }
        if (polygon.getSize() >= 2) {
            polygonRasterizer.rasterize(polygon, COLOR_POLYGON);
        } else if (polygon.getSize() == 1) {
            Point p = polygon.getFirst();
            panel.getRaster().setPixel(p.getX(), p.getY(), COLOR_POLYGON);
        }
    }

    private void drawPreview() {
        if (editingVertex) {
            return;
        }
        if (!drawing || startPoint == null || previewPoint == null) {
            return;
        }

        if (mode == Mode.LINE) {
            lineRasterizer.rasterize(
                    startPoint.getX(), startPoint.getY(),
                    previewPoint.getX(), previewPoint.getY(),
                    COLOR_PREVIEW
            );
            return;
        }

        if (polygon.getSize() == 0) {
            lineRasterizer.rasterize(
                    startPoint.getX(), startPoint.getY(),
                    previewPoint.getX(), previewPoint.getY(),
                    COLOR_PREVIEW
            );
            return;
        }

        if (polygon.getSize() == 1) {
            Point first = polygon.getFirst();
            lineRasterizer.rasterize(
                    first.getX(), first.getY(),
                    previewPoint.getX(), previewPoint.getY(),
                    COLOR_PREVIEW
            );
            return;
        }

        if (insertEdgeIndex >= 0) {
            Point a = polygon.getPoint(insertEdgeIndex);
            Point b = polygon.getPoint((insertEdgeIndex + 1) % polygon.getSize());
            lineRasterizer.rasterize(
                    a.getX(), a.getY(),
                    previewPoint.getX(), previewPoint.getY(),
                    COLOR_PREVIEW
            );
            lineRasterizer.rasterize(
                    b.getX(), b.getY(),
                    previewPoint.getX(), previewPoint.getY(),
                    COLOR_PREVIEW
            );
        }
    }
}

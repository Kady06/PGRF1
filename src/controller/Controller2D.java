package controller;

import model.Line;
import model.Point;
import model.Polygon;
import raster.RasterBufferedImage;
import rasterize.LineRasterizer;
import rasterize.LineRasterizerTrivial;
import rasterize.PolygonRasterizer;
import rasterize.ScanLineFiller;
import rasterize.SeedFiller;
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
    private static final int COLOR_BOUNDARY = 0xff0000;
    private static final int COLOR_FILL = 0x0088ff;
    private static final int COLOR_SCAN_FILL = 0xff6600;
    private static final int COLOR_BACKGROUND = 0x000000;
    private static final double VERTEX_PICK_RADIUS = 12.0;

    private enum Mode {
        LINE, POLYGON, FILL
    }

    private final Panel panel;
    private final LineRasterizer lineRasterizer;
    private final LineRasterizer boundaryRasterizer;
    private final PolygonRasterizer polygonRasterizer;
    private final ScanLineFiller scanLineFiller;
    /** Trvalá vrstva hranic a výplní – nepřebíjí ji překreslení úseček/n-úhelníku. */
    private final RasterBufferedImage paintLayer;

    private final List<Line> lines = new ArrayList<>();
    /** Dokončené n-úhelníky (uložené přes Enter). */
    private final List<Polygon> polygons = new ArrayList<>();
    /** Indexy dokončených n-úhelníků se scan-line výplní. */
    private final List<Boolean> polygonsScanFilled = new ArrayList<>();
    /** Aktuálně rozpracovaný n-úhelník. */
    private Polygon polygon = new Polygon();

    private Mode mode = Mode.LINE;
    private SeedFiller.Condition fillCondition = SeedFiller.Condition.BACKGROUND;
    private boolean scanLineFillActive;

    private boolean drawing;
    private Point startPoint;
    private Point previewPoint;
    private Point rawMousePoint;

    private int insertEdgeIndex = -1;

    private boolean editingVertex;
    private int editedVertexIndex = -1;

    private boolean drawingBoundary;
    private Point lastBoundaryPoint;

    /** Poslední známá pozice kurzoru na plátně (pro scan-line pod kurzorem). */
    private Point cursorPoint;

    public Controller2D(Panel panel) {
        this.panel = panel;
        this.paintLayer = new RasterBufferedImage(panel.getRaster().getWidth(), panel.getRaster().getHeight());
        this.lineRasterizer = new LineRasterizerTrivial(panel.getRaster());
        this.boundaryRasterizer = new LineRasterizerTrivial(paintLayer);
        this.polygonRasterizer = new PolygonRasterizer(lineRasterizer);
        this.scanLineFiller = new ScanLineFiller(lineRasterizer);
        initListeners();
        redraw();
    }

    private void initListeners() {
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                cursorPoint = clampToCanvas(e);
                if (mode == Mode.FILL) {
                    handleFillMousePressed(e);
                    return;
                }
                Point mouse = cursorPoint;
                if (e.getButton() == MouseEvent.BUTTON3) {
                    startVertexEdit(mouse.getX(), mouse.getY());
                    return;
                }
                if (e.getButton() != MouseEvent.BUTTON1 || editingVertex) {
                    return;
                }

                drawing = true;
                startPoint = mouse;
                rawMousePoint = startPoint;
                previewPoint = startPoint;
                updateInsertEdgePreview();
                redraw();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (mode == Mode.FILL) {
                    handleFillMouseReleased(e);
                    return;
                }
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
                rawMousePoint = clampToCanvas(e);
                Point end = resolveEndPoint(rawMousePoint, e.isShiftDown());

                if (mode == Mode.LINE) {
                    lines.add(new Line(startPoint, end, COLOR_LINE));
                } else {
                    addPolygonVertex(end);
                }

                startPoint = null;
                previewPoint = null;
                rawMousePoint = null;
                insertEdgeIndex = -1;
                redraw();
            }
        });

        panel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                cursorPoint = clampToCanvas(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                cursorPoint = clampToCanvas(e);
                if (mode == Mode.FILL) {
                    handleFillMouseDragged(e);
                    return;
                }
                Point mouse = cursorPoint;
                if (editingVertex && editedVertexIndex >= 0) {
                    polygon.getPoint(editedVertexIndex).set(mouse.getX(), mouse.getY());
                    redraw();
                    return;
                }
                if (!drawing) {
                    return;
                }
                rawMousePoint = mouse;
                previewPoint = resolveEndPoint(rawMousePoint, e.isShiftDown());
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
                    case KeyEvent.VK_F -> {
                        cancelInteraction();
                        mode = Mode.FILL;
                        System.out.println("Režim: semínkové vyplnění (F) – LMB hranice, RMB výplň; T přepne podmínku");
                        System.out.println("Podmínka: " + fillConditionLabel());
                        redraw();
                    }
                    case KeyEvent.VK_T -> {
                        toggleFillCondition();
                    }
                    case KeyEvent.VK_S -> {
                        applyScanLineFill();
                    }
                    case KeyEvent.VK_ENTER -> {
                        finalizePolygon();
                    }
                    case KeyEvent.VK_SHIFT -> refreshLinePreview(true);
                    default -> {
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                    refreshLinePreview(false);
                }
            }
        });
    }

    private void handleFillMousePressed(MouseEvent e) {
        Point mouse = clampToCanvas(e);
        if (e.getButton() == MouseEvent.BUTTON3) {
            applySeedFill(mouse.getX(), mouse.getY());
            return;
        }
        if (e.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        drawingBoundary = true;
        lastBoundaryPoint = mouse;
        paintLayer.setPixel(lastBoundaryPoint.getX(), lastBoundaryPoint.getY(), COLOR_BOUNDARY);
        panel.getRaster().setPixel(lastBoundaryPoint.getX(), lastBoundaryPoint.getY(), COLOR_BOUNDARY);
        panel.repaint();
    }

    private void handleFillMouseDragged(MouseEvent e) {
        if (!drawingBoundary || lastBoundaryPoint == null) {
            return;
        }
        Point next = clampToCanvas(e);
        if (lastBoundaryPoint.getX() == next.getX() && lastBoundaryPoint.getY() == next.getY()) {
            return;
        }
        // Trvale do paintLayer + okamžitě na plátno (bez clear, výplň zůstane).
        boundaryRasterizer.rasterize(
                lastBoundaryPoint.getX(), lastBoundaryPoint.getY(),
                next.getX(), next.getY(),
                COLOR_BOUNDARY
        );
        lineRasterizer.rasterize(
                lastBoundaryPoint.getX(), lastBoundaryPoint.getY(),
                next.getX(), next.getY(),
                COLOR_BOUNDARY
        );
        lastBoundaryPoint = next;
        panel.repaint();
    }

    private void handleFillMouseReleased(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1 || !drawingBoundary) {
            return;
        }
        drawingBoundary = false;
        lastBoundaryPoint = null;
    }

    private void applySeedFill(int x, int y) {
        // Pracovní rastr: stávající hranice/výplně + obrys n-úhelníku/úseček,
        // aby seed fill „viděl“ geometrii (ne jen freehand paintLayer).
        RasterBufferedImage work = new RasterBufferedImage(paintLayer.getWidth(), paintLayer.getHeight());
        work.copyFrom(paintLayer);
        stampGeometry(work, fillCondition == SeedFiller.Condition.BOUNDARY);

        int limitColor = fillCondition == SeedFiller.Condition.BACKGROUND
                ? work.getPixel(x, y)
                : COLOR_BOUNDARY;

        new SeedFiller(work).fill(x, y, COLOR_FILL, fillCondition, limitColor);
        // Trvale uložíme jen výslednou výplň (geometrie zůstává ve vektorech).
        work.copyColorTo(paintLayer, COLOR_FILL);

        redraw();
        System.out.println("Seed fill @ (" + x + ", " + y + "), " + fillConditionLabel());
    }

    /**
     * Vykreslí úsečky a n-úhelník do cílového rastru jako bariéry pro seed fill.
     * V režimu BOUNDARY se kreslí barvou hranice, jinak původními barvami.
     */
    private void stampGeometry(RasterBufferedImage target, boolean asBoundary) {
        LineRasterizer stampLines = new LineRasterizerTrivial(target);
        PolygonRasterizer stampPolygon = new PolygonRasterizer(stampLines);

        int lineColor = asBoundary ? COLOR_BOUNDARY : COLOR_LINE;
        int polygonColor = asBoundary ? COLOR_BOUNDARY : COLOR_POLYGON;

        for (Line line : lines) {
            stampLines.rasterize(line.getX1(), line.getY1(), line.getX2(), line.getY2(), lineColor);
        }

        for (Polygon finished : polygons) {
            stampPolygonShape(stampPolygon, target, finished, polygonColor);
        }
        stampPolygonShape(stampPolygon, target, polygon, polygonColor);
    }

    private void stampPolygonShape(
            PolygonRasterizer stampPolygon,
            RasterBufferedImage target,
            Polygon shape,
            int color
    ) {
        if (shape.getSize() >= 2) {
            stampPolygon.rasterize(shape, color);
        } else if (shape.getSize() == 1) {
            Point p = shape.getFirst();
            target.setPixel(p.getX(), p.getY(), color);
        }
    }

    private void toggleFillCondition() {
        fillCondition = fillCondition == SeedFiller.Condition.BACKGROUND
                ? SeedFiller.Condition.BOUNDARY
                : SeedFiller.Condition.BACKGROUND;
        System.out.println("Podmínka vyplnění: " + fillConditionLabel());
    }

    private void applyScanLineFill() {
        Point cursor = cursorPoint;
        if (cursor == null) {
            java.awt.Point mouse = panel.getMousePosition();
            if (mouse != null) {
                cursor = clampToCanvas(mouse.x, mouse.y);
            }
        }
        if (cursor == null) {
            System.out.println("Scan-line: kurzor není nad plátnem");
            return;
        }

        int x = cursor.getX();
        int y = cursor.getY();

        // Od shora (aktuální / naposledy uložený) – vyplní n-úhelník pod kurzorem.
        if (polygon.getSize() >= 3 && polygon.contains(x, y)) {
            scanLineFillActive = true;
            redraw();
            System.out.println("Scan-line: vyplněn aktuální n-úhelník pod kurzorem");
            return;
        }

        for (int i = polygons.size() - 1; i >= 0; i--) {
            Polygon candidate = polygons.get(i);
            if (candidate.getSize() >= 3 && candidate.contains(x, y)) {
                polygonsScanFilled.set(i, true);
                redraw();
                System.out.println("Scan-line: vyplněn n-úhelník #" + (i + 1) + " pod kurzorem");
                return;
            }
        }

        System.out.println("Scan-line: pod kurzorem není žádný n-úhelník");
    }

    /** Uloží aktuální n-úhelník a začne kreslit nový. */
    private void finalizePolygon() {
        if (mode != Mode.POLYGON) {
            mode = Mode.POLYGON;
        }
        cancelInteraction();

        if (polygon.getSize() < 3) {
            System.out.println("Enter: n-úhelník musí mít alespoň 3 vrcholy");
            return;
        }

        polygons.add(polygon);
        polygonsScanFilled.add(scanLineFillActive);
        polygon = new Polygon();
        scanLineFillActive = false;
        redraw();
        System.out.println("N-úhelník uložen (Enter), můžete kreslit nový");
    }

    private String fillConditionLabel() {
        return fillCondition == SeedFiller.Condition.BACKGROUND
                ? "barva pozadí (záplava)"
                : "barva hranice";
    }

    /** Ořízne souřadnice myši na rozměry kreslicího rastru. */
    private Point clampToCanvas(MouseEvent e) {
        return clampToCanvas(e.getX(), e.getY());
    }

    private Point clampToCanvas(int x, int y) {
        int maxX = panel.getRaster().getWidth() - 1;
        int maxY = panel.getRaster().getHeight() - 1;
        return new Point(
                Math.max(0, Math.min(x, maxX)),
                Math.max(0, Math.min(y, maxY))
        );
    }

    private Point resolveEndPoint(Point mouse, boolean shiftDown) {
        if (mode == Mode.LINE && shiftDown && startPoint != null) {
            return snapAxisAlignedOrDiagonal(startPoint, mouse);
        }
        return mouse;
    }

    private Point snapAxisAlignedOrDiagonal(Point start, Point mouse) {
        int sx = start.getX();
        int sy = start.getY();
        int mx = mouse.getX();
        int my = mouse.getY();

        Point horizontal = clampToCanvas(mx, sy);
        Point vertical = clampToCanvas(sx, my);

        int tPlus = (int) Math.round(((mx - sx) + (my - sy)) / 2.0);
        Point diagonalPlus = clampToCanvas(sx + tPlus, sy + tPlus);

        int tMinus = (int) Math.round(((mx - sx) - (my - sy)) / 2.0);
        Point diagonalMinus = clampToCanvas(sx + tMinus, sy - tMinus);

        Point best = horizontal;
        double bestDist = mouse.distanceTo(horizontal);

        Point[] candidates = {vertical, diagonalPlus, diagonalMinus};
        for (Point candidate : candidates) {
            double dist = mouse.distanceTo(candidate);
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }
        return best;
    }

    private void refreshLinePreview(boolean shiftDown) {
        if (!drawing || mode != Mode.LINE || startPoint == null || rawMousePoint == null) {
            return;
        }
        previewPoint = resolveEndPoint(rawMousePoint, shiftDown);
        redraw();
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
        rawMousePoint = null;
        insertEdgeIndex = -1;
        drawingBoundary = false;
        lastBoundaryPoint = null;
    }

    private void cancelInteraction() {
        cancelDrawing();
        editingVertex = false;
        editedVertexIndex = -1;
    }

    private void clearAll() {
        lines.clear();
        polygons.clear();
        polygonsScanFilled.clear();
        polygon = new Polygon();
        scanLineFillActive = false;
        cancelInteraction();
        paintLayer.clear();
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
        for (int i = 0; i < polygons.size(); i++) {
            List<Polygon> occluders = occludersAbove(i);
            drawPolygon(
                    polygons.get(i),
                    Boolean.TRUE.equals(polygonsScanFilled.get(i)),
                    occluders
            );
        }
        drawPolygon(polygon, scanLineFillActive, List.of());
        // Hranice + výplně z trvalé vrstvy (přežijí dokreslování i změnu režimu).
        paintLayer.copyNonBackgroundTo(panel.getRaster(), COLOR_BACKGROUND);
    }

    /** N-úhelníky kreslené později (zakrývají výplň nižších). */
    private List<Polygon> occludersAbove(int finishedIndex) {
        List<Polygon> occluders = new ArrayList<>();
        for (int j = finishedIndex + 1; j < polygons.size(); j++) {
            occluders.add(polygons.get(j));
        }
        if (polygon.getSize() >= 3) {
            occluders.add(polygon);
        }
        return occluders;
    }

    private void drawPolygon(Polygon shape, boolean scanFilled, List<Polygon> occluders) {
        if (scanFilled && shape.getSize() >= 3) {
            scanLineFiller.fill(shape, COLOR_SCAN_FILL, occluders);
        }
        if (shape.getSize() >= 2) {
            polygonRasterizer.rasterize(shape, COLOR_POLYGON);
        } else if (shape.getSize() == 1) {
            Point p = shape.getFirst();
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

package rasterize;

import raster.Raster;

import java.util.ArrayDeque;
import java.util.Deque;


public class SeedFiller {

    public enum Condition {
        BACKGROUND,
        BOUNDARY
    }

    private final Raster raster;

    public SeedFiller(Raster raster) {
        this.raster = raster;
    }

    public void fill(int x, int y, int fillColor, Condition condition, int limitColor) {
        if (!inBounds(x, y)) {
            return;
        }

        int fill = rgb(fillColor);
        int limit = rgb(limitColor);

        if (condition == Condition.BACKGROUND) {
            fillBackground(x, y, fill, limit);
        } else {
            fillBoundary(x, y, fill, limit);
        }
    }

    private void fillBackground(int seedX, int seedY, int fill, int background) {
        if (background == fill) {
            return;
        }
        if (rgb(raster.getPixel(seedX, seedY)) != background) {
            return;
        }

        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{seedX, seedY});

        while (!stack.isEmpty()) {
            int[] p = stack.pop();
            int x = p[0];
            int y = p[1];
            if (!inBounds(x, y)) {
                continue;
            }
            if (rgb(raster.getPixel(x, y)) != background) {
                continue;
            }

            raster.setPixel(x, y, fill);
            stack.push(new int[]{x + 1, y});
            stack.push(new int[]{x - 1, y});
            stack.push(new int[]{x, y + 1});
            stack.push(new int[]{x, y - 1});
        }
    }

    private void fillBoundary(int seedX, int seedY, int fill, int boundary) {
        if (boundary == fill) {
            return;
        }

        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{seedX, seedY});

        while (!stack.isEmpty()) {
            int[] p = stack.pop();
            int x = p[0];
            int y = p[1];
            if (!inBounds(x, y)) {
                continue;
            }

            int pixel = rgb(raster.getPixel(x, y));
            if (pixel == boundary || pixel == fill) {
                continue;
            }

            raster.setPixel(x, y, fill);
            stack.push(new int[]{x + 1, y});
            stack.push(new int[]{x - 1, y});
            stack.push(new int[]{x, y + 1});
            stack.push(new int[]{x, y - 1});
        }
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < raster.getWidth() && y >= 0 && y < raster.getHeight();
    }

    private static int rgb(int color) {
        return color & 0xFFFFFF;
    }
}

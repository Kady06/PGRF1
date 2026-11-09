package raster;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class RasterBufferedImage implements Raster {

    private final BufferedImage image;
    private int clearColor = 0x000000;

    public RasterBufferedImage(int width, int height) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    @Override
    public void setPixel(int x, int y, int color) {
        if (x >= 0 && x < getWidth() && y >= 0 && y < getHeight()) {
            image.setRGB(x, y, color);
        }
    }

    @Override
    public int getPixel(int x, int y) {
        if (x >= 0 && x < getWidth() && y >= 0 && y < getHeight()) {
            return image.getRGB(x, y);
        }
        return clearColor;
    }

    @Override
    public int getWidth() {
        return image.getWidth();
    }

    @Override
    public int getHeight() {
        return image.getHeight();
    }

    @Override
    public void clear() {
        Graphics g = image.getGraphics();
        g.setColor(new java.awt.Color(clearColor));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();
    }

    @Override
    public void setClearColor(int color) {
        this.clearColor = color;
    }

    public BufferedImage getImage() {
        return image;
    }

    /** Zkopíruje do cíle pixely, které nejsou barvou pozadí. */
    public void copyNonBackgroundTo(Raster dest, int backgroundColor) {
        int bg = backgroundColor & 0xFFFFFF;
        int w = getWidth();
        int h = getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int color = image.getRGB(x, y) & 0xFFFFFF;
                if (color != bg) {
                    dest.setPixel(x, y, color);
                }
            }
        }
    }

    public void copyFrom(RasterBufferedImage src) {
        Graphics g = image.getGraphics();
        g.drawImage(src.getImage(), 0, 0, null);
        g.dispose();
    }

    /** Zkopíruje do cíle jen pixely dané barvy. */
    public void copyColorTo(Raster dest, int color) {
        int wanted = color & 0xFFFFFF;
        int w = getWidth();
        int h = getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((image.getRGB(x, y) & 0xFFFFFF) == wanted) {
                    dest.setPixel(x, y, wanted);
                }
            }
        }
    }
}

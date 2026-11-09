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
}

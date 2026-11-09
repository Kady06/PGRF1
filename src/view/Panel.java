package view;

import raster.Raster;
import raster.RasterBufferedImage;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;

public class Panel extends JPanel {

    private final RasterBufferedImage raster;

    public Panel(int width, int height) {
        setPreferredSize(new Dimension(width, height));
        raster = new RasterBufferedImage(width, height);
        raster.clear();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(raster.getImage(), 0, 0, null);
    }

    public Raster getRaster() {
        return raster;
    }

    public void clear() {
        raster.clear();
        repaint();
    }
}

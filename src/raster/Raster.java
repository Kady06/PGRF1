package raster;

public interface Raster {

    void setPixel(int x, int y, int color);

    int getPixel(int x, int y);

    int getWidth();

    int getHeight();

    void clear();

    void setClearColor(int color);
}

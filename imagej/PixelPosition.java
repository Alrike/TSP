package de.geomar.imagej;

public class PixelPosition {

    private final int x;
    private final int y;
    private final int dimension;

    public PixelPosition(int dimension, int x, int y) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getDimension() {
        return dimension;
    }
}

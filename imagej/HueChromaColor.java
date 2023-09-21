package de.geomar.imagej;

public class HueChromaColor {
    private final double hue;
    private final double chroma;
    private final double diff;

    public HueChromaColor(double hue, double chroma, double diff) {
        this.hue = hue;
        this.chroma = chroma;
        this.diff = diff;
    }

    public double getHue() {
        return hue;
    }

    public double getChroma() {
        return chroma;
    }

    public double getDiff() {
        return diff;
    }

}
package de.geomar.imagej;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SourceImage {
    private int index;
    private int width;
    private int height;
    private HashMap<Integer, RgbPixel> pixels;
    private List<HueChromaColor> convertedPixels;
    private HashMap<RgbColor, List<PixelPosition>> colors;

    public SourceImage(int index, int width, int height) {
        this.index = index;
        this.width = width;
        this.height = height;
        this.pixels = new HashMap<>(width * height);
        this.convertedPixels = new ArrayList<>(width * height);
        this.colors = new HashMap<>();
    }


    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public HashMap<Integer, RgbPixel> getPixels() {
        return pixels;
    }

    public List<HueChromaColor> getConvertedPixels() {
        return convertedPixels;
    }

    public void setConvertedPixels(List<HueChromaColor> convertedPixels) {
        this.convertedPixels = convertedPixels;
    }

    public int getIndex() {
        return index;
    }

    public HashMap<RgbColor, List<PixelPosition>> getColors() {
        return colors;
    }
}

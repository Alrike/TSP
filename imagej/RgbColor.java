package de.geomar.imagej;

import java.util.Objects;

public class RgbColor {
    private final short red;
    private final short green;
    private final short blue;

    public RgbColor(short red, short green, short blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public short getRed() {
        return red;
    }

    public short getGreen() {
        return green;
    }

    public short getBlue() {
        return blue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RgbColor rgbColor = (RgbColor) o;
        return getRed() == rgbColor.getRed() && getGreen() == rgbColor.getGreen() && getBlue() == rgbColor.getBlue();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRed(), getGreen(), getBlue());
    }
}

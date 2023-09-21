package de.geomar.imagej;

public class OutputPixel {
    
    private final int image;
    private final int positionX;
    private final int positionY;
    private final byte value;
    private final OutputImage target;

    public OutputPixel(int image, int positionX, int positionY, byte value, OutputImage target) {
        this.image = image;
        this.positionX = positionX;
        this.positionY = positionY;
        this.value = value;
        this.target = target;
    }

    public int getImage() {
        return image;
    }

    public byte getValue() {
        return value;
    }

    public OutputImage getTarget() {
        return target;
    }

    public int getPositionX() {
        return positionX;
    }

    public int getPositionY() {
        return positionY;
    }
}

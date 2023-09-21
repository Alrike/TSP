/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package de.geomar.imagej;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.File;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This example illustrates how to create an ImageJ {@link Command} plugin.
 * <p>
 * The code here is a simple Gaussian blur using ImageJ Ops.
 * </p>
 * <p>
 * You should replace the parameter fields with your own inputs and outputs,
 * and replace the {@link run} method implementation with your own logic.
 * </p>
 *
 * @param <T>
 */
@Plugin(type = Command.class, menuPath = "Plugins>TEP/CSP")
public class TEP_CSP<T extends UnsignedByteType> implements Command {

    @Parameter
    private Dataset currentData;

    @Parameter
    private UIService uiService;

    @Parameter
    private OpService opService;

    @Parameter
    private StatusService statusService;

    @Parameter
    private ImageJ imageJ;


    @Override
    public void run() {
        final ImgPlus<T> image = (ImgPlus<T>) currentData.getImgPlus();

        try {
            long startMillis = System.currentTimeMillis();
            SourceImage[] sourceImages = readImages(image);
            int imageCount = sourceImages.length;

            long diffMillis = System.currentTimeMillis() - startMillis;
            System.out.println("Read pixel in " + diffMillis + "ms");

            // merge colors into one hashmap
            startMillis = System.currentTimeMillis();
            Map<RgbColor, List<PixelPosition>> positions = new HashMap<>();
            for (SourceImage sourceImage : sourceImages) {
                System.out.println("Found " + sourceImage.getColors().size() + " colors on image "+ sourceImage.getIndex());

                for (Map.Entry<RgbColor, List<PixelPosition>> entry : sourceImage.getColors().entrySet()) {
                    List<PixelPosition> colorPositions = positions.getOrDefault(entry.getKey(), entry.getValue());

                    if (!positions.containsKey(entry.getKey())) {
                        positions.put(entry.getKey(), colorPositions);
                        continue;
                    }

                    colorPositions.addAll(entry.getValue());
                }
            }
            diffMillis = System.currentTimeMillis() - startMillis;
            System.out.println("Merged " + positions.size() + " colors in " + diffMillis + "ms");

            // release reference to read data
            sourceImages = null;
            System.gc();

            // Convert colors
            startMillis = System.currentTimeMillis();
            ConcurrentHashMap<RgbColor, HueChromaColor> convertedColors = new ConcurrentHashMap<>();
            positions.keySet().parallelStream().forEach(c -> convertedColors.computeIfAbsent(c, this::convertPixel));
            diffMillis = System.currentTimeMillis() - startMillis;
            System.out.println("Converted " + convertedColors.size() + "  colors in " + diffMillis + "ms");

            // Separate data into output images
            startMillis = System.currentTimeMillis();
            Map<OutputImage, List<OutputPixel>> images = positions.entrySet()
                    .parallelStream()
                    .flatMap(c -> convertToOutputPixels(c, convertedColors.get(c.getKey())))
                    .collect(Collectors.groupingBy(p -> p.getTarget()));
            diffMillis = System.currentTimeMillis() - startMillis;
            System.out.println("Separated to target images in " + diffMillis + "ms");

            // Generate output images
            Arrays.stream(OutputImage.values()).filter(t -> t != OutputImage.NONE)
                    .parallel()
                    .map(type -> generateOutputImage(type, images.get(type), imageCount))
                    .forEach(uiService::show);

            // release resources
            convertedColors.clear();
            positions.clear();
            images.clear();
            System.gc();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Stream<OutputPixel> convertToOutputPixels(Map.Entry<RgbColor, List<PixelPosition>> c, HueChromaColor color) {
        double pixelHue = color.getHue();

        if (pixelHue > 20 && pixelHue < 70) { //case Detritus
            return c.getValue().stream().map(e -> new OutputPixel(
                    e.getDimension(),
                    e.getX(),
                    e.getY(),
                    (byte) Math.round(color.getChroma() * 255),
                    OutputImage.DEBRIS));
        } else if (pixelHue > 80 && pixelHue < 160) { //case Algae
            return c.getValue().stream().map(e -> new OutputPixel(
                    e.getDimension(),
                    e.getX(),
                    e.getY(),
                    (byte) Math.round(color.getChroma() * 255),
                    OutputImage.ALGAE));
        } else if (pixelHue > 170 && pixelHue < 220) {//case TEP
            return c.getValue().stream().map(e -> new OutputPixel(
                    e.getDimension(),
                    e.getX(),
                    e.getY(),
                    (byte) Math.round(color.getDiff() * 255),
                    OutputImage.TEP));
        } else if (pixelHue > 221 && pixelHue < 285) { //case CSP
            return c.getValue().stream().map(e -> new OutputPixel(
                    e.getDimension(),
                    e.getX(),
                    e.getY(),
                    (byte) Math.round(color.getChroma() * 255),
                    OutputImage.CSP));
        }

        return new ArrayList<OutputPixel>().stream();
    }

    SourceImage[] readImages(ImgPlus<T> image) {
        // if the image is not a stack, we want to handle if as like a stack with one image
        boolean isStack = image.dimensionsAsLongArray().length == 4;
        int images = isStack
                ? (int) image.dimensionsAsLongArray()[3] : 1;
        SourceImage[] sourceImages = (SourceImage[]) Array.newInstance(SourceImage.class, images);
        for (int i = 0; i < sourceImages.length; i++) {
            sourceImages[i] = new SourceImage(i, (int) currentData.getWidth(), (int) currentData.getHeight());
        }

        AtomicInteger counter = new AtomicInteger(0);

        Arrays.stream(sourceImages).parallel().forEach(i -> {
            for (int x = 0; x < currentData.getWidth(); x++) {
                for (int y = 0; y < currentData.getHeight(); y++) {
                    short red = (short) (image.getAt(getPixelPosition(isStack, x, y, 0, i.getIndex())).getByte() & 0xff);
                    short green = (short) (image.getAt(getPixelPosition(isStack, x, y, 1, i.getIndex())).getByte() & 0xff);
                    short blue = (short) (image.getAt(getPixelPosition(isStack, x, y, 2, i.getIndex())).getByte() & 0xff);

                    RgbColor color = new RgbColor(red, green, blue);
                    if (!i.getColors().containsKey(color)) {
                        i.getColors().put(color, new ArrayList<>());
                    }

                    i.getColors().get(color).add(new PixelPosition(i.getIndex(), x, y));
                    statusService.showProgress(counter.getAndIncrement(), (int) image.size());
                }
            }
        });

        return sourceImages;
    }

    long[] getPixelPosition(boolean isStack, int x, int y, int color, int dimension) {
        return isStack ? new long[] {x, y, color, dimension} : new long[] {x, y, color};
    }

    HueChromaColor convertPixel(RgbColor rgb) {
        double red = rgb.getRed() / 255f;
        double green = rgb.getGreen() / 255f;
        double blue = rgb.getBlue() / 255f;

        double pixelChroma, pixelHue, pixelDiff;

        double min = Math.min(red, Math.min(green, blue));
        double max = Math.max(red, Math.max(green, blue));
        pixelChroma = max - min;

        //check the different cases and calculate hue+chroma
        if (max == green) { //greenmax
            if (pixelChroma == 0) { //if red=green AND green=blue => chroma is 0, therefore, catch here
                pixelHue = 0;
            } else {
                pixelHue = 60 * (((blue - red) / pixelChroma) + 2);
            }
        } else if (max == red) { //redmax
            pixelHue = 60 * workingModulo((green - blue) / pixelChroma, 6);
        } else { //bluemax
            pixelHue = 60 * (((red - green) / pixelChroma) + 4);
        }

        pixelDiff = blue - red;

        return new HueChromaColor(pixelHue, pixelChroma, pixelDiff);
    }

    /*
     * Apperantly the % operator does not return the correct modulo (as it only calculates the remainder).
     * Therefor we use the (actual) Modulo operation (Math.mod). 😜
     * It seems that it only works for integers, in oder to compensate that we multiply the values beforehand and
     * divide the result by 1000000.
     */
    double workingModulo(double dividend, double divisor) {
        long workingDividend = (long) (dividend * 1000000);
        long workingDivisor = (long) (divisor * 1000000);

        return Math.floorMod(workingDividend, workingDivisor) / 1000000d;
    }

    ImgPlus<T> generateOutputImage(OutputImage imageType, List<OutputPixel> pixels, int dimensions) {
        Img<DoubleType> tempImg = opService.create().img(
                new long[] {
                        currentData.getWidth(), currentData.getHeight(), dimensions});
        ImgPlus<T> output = new ImgPlus(opService.create().img(tempImg, new UnsignedByteType()), imageType.getTitle());

        for (OutputPixel pixel : pixels) {
            output.getAt(pixel.getPositionX(), pixel.getPositionY(), pixel.getImage()).set(pixel.getValue());
        }

        return output;
    }

    /**
     * This main function serves for development purposes.
     * It allows you to run the plugin immediately out of
     * your integrated development environment (IDE).
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        //IJ = ij;
        // ask the user for a file to open
        final File file = ij.ui().chooseFile(null, "open");

        if (file != null) {
            // load the dataset
            final Dataset dataset = ij.scifio().datasetIO().open(file.getPath());

            // show the image
            ij.ui().show(dataset);

            // invoke the plugin
            ij.command().run(TEP_CSP.class, true);
        }
    }
}

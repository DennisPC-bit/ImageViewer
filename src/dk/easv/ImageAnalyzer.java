package dk.easv;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class ImageAnalyzer implements Runnable, Callable<Map<String, Integer>> {
    Image image;
    int startHeight;
    int endHeight;
    int startWidth;
    int endWidth;
    public static Map<String, Integer> colorIntegerHashMap = new HashMap<>();

    public ImageAnalyzer(Image image, int startWidth, int endWidth, int startHeight, int endHeight) {
        this.image = image;
        this.startWidth = startWidth;
        this.endWidth = endWidth;
        this.startHeight =startHeight;
        this.endHeight=endHeight;
    }

    @Override
    public void run() {
        call();
    }

    public static Map<String, Integer> getColorIntegerHashMap() {
        return colorIntegerHashMap;
    }

    @Override
    public Map<String, Integer> call() {
        int blueCount = 0;
        int redCount = 0;
        int greenCount = 0;
        int mixedCount = 0;

        for (int i = startWidth; i < endWidth; i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                Color color = image.getPixelReader().getColor(i, j);
                double blue = color.getBlue();
                double green = color.getGreen();
                double red = color.getRed();
                if (blue > Math.max(green, red))
                    blueCount++;
                else if (green > Math.max(red, blue))
                    greenCount++;
                else if (red > Math.max(blue, green))
                    redCount++;
                else
                    mixedCount++;
            }
        }

        colorIntegerHashMap.put("MIXED", colorIntegerHashMap.getOrDefault("MIXED", 0) + mixedCount);
        colorIntegerHashMap.put("RED", colorIntegerHashMap.getOrDefault("RED", 0) + redCount);
        colorIntegerHashMap.put("BLUE", colorIntegerHashMap.getOrDefault("BLUE", 0) + blueCount);
        colorIntegerHashMap.put("GREEN", colorIntegerHashMap.getOrDefault("GREEN", 0) + greenCount);
        return colorIntegerHashMap;
    }
}

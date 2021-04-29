package dk.easv;

import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class ImageAnalyzer extends Task<Map<String, Integer>> implements Callable<Map<String, Integer>> {
    Image image;
    int startHeight;
    int endHeight;
    int startWidth;
    int endWidth;
    private static Map<String, Integer> colorMap = Collections.synchronizedMap(new HashMap<>());

    public ImageAnalyzer(Image image, int startWidth, int endWidth, int startHeight, int endHeight) {
        this.image = image;
        this.startWidth = startWidth;
        this.endWidth = endWidth;
        this.startHeight = startHeight;
        this.endHeight = endHeight;
    }

    @Override
    public void run() {
        call();
    }

    public static void clearColorMap(){
        colorMap = Collections.synchronizedMap(new HashMap<>());
    }

    public static Map<String, Integer> getColorMap() {
        return colorMap;
    }

    @Override
    public synchronized Map<String, Integer> call() {
        int blueCount = 0;
        int greenCount = 0;
        int redCount = 0;
        int greyCount = 0;
        for (int i = startWidth; i < endWidth; i++) {
            for (int j = startHeight; j < endHeight; j++) {
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
                    greyCount++;

            }
        }
        colorMap.put("Blue", colorMap.getOrDefault("Blue", 0) + blueCount);
        colorMap.put("Green", colorMap.getOrDefault("Green", 0) + greenCount);
        colorMap.put("Red", colorMap.getOrDefault("Red", 0) + redCount);
        colorMap.put("Grey", colorMap.getOrDefault("Grey", 0) + greyCount);
        return colorMap;
    }
}
